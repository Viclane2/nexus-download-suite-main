package core;import threading.MoteurTelechargement;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Représente une tâche de téléchargement RÉEL unique.
 *
 * Corrections intégrées :
 *  1. REPRISE — si des fichiers .partX existent, on reprend depuis les octets déjà téléchargés
 *  2. MULTI-SEGMENTS — 4 threads parallèles avec Range: bytes=X-Y (style IDM)
 *  3. INTÉGRITÉ — calcul SHA-256 du fichier final après fusion
 */
public class TacheTelechargement implements Runnable, Serializable, ITask {

    private static final long serialVersionUID = 4L;
    private static final DateTimeFormatter FORMAT_HEURE = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int  TAILLE_BLOC              = 8192;   // 8 Ko
    private static final long INTERVALLE_NOTIFICATION_MS = 120;
    private static final int  NB_SEGMENTS              = 4;
    private static final long INTERVALLE_SAUVEGARDE_MS = 5000;   // sauvegarde BD toutes les 5 s

    private final String id;
    private final String nomFichier;
    private final String urlSource;
    private final String cheminDestination;

    private volatile double      tailleTotaleMo;
    private volatile double      progression;
    private volatile long        octetsRecus;
    private volatile StatutTache statut;
    private          LocalDateTime dateDebut;
    private          LocalDateTime dateFin;
    private volatile double      vitesseMoS  = 0.0;
    private volatile long        etaSecondes = -1;
    private volatile String      hashSha256  = null;   // ← CORRECTION 3

    private transient ProgressionListener    listener;
    private transient GestionnaireTaches     gestionnaire;
    private transient volatile MoteurTelechargement moteur;
    private transient volatile boolean       annulee = false;

    // ─── Constructeur normal ────────────────────────────────────────────────
    public TacheTelechargement(String nomFichier, String urlSource, String cheminDestination) {
        this.id                 = UUID.randomUUID().toString();
        this.nomFichier         = nomFichier;
        this.urlSource          = urlSource;
        this.cheminDestination  = cheminDestination;
        this.tailleTotaleMo     = -1;
        this.progression        = 0.0;
        this.statut             = StatutTache.EN_ATTENTE;
        this.octetsRecus        = 0;
    }

    // ─── Setters ─────────────────────────────────────────────────────────────
    public void setListener(ProgressionListener listener) { this.listener = listener; }
    public void setGestionnaire(GestionnaireTaches g)     { this.gestionnaire = g; }
    public void setMoteur(MoteurTelechargement m)          { this.moteur = m; }

    // ─── run() principal ─────────────────────────────────────────────────────
    @Override
    public void run() {
        this.statut    = StatutTache.EN_COURS;
        this.dateDebut = LocalDateTime.now();
        notifierProgression();

        try {
            // ── Handshake : taille + support Range ──────────────────────────
            URL urlObj;
            try {
                urlObj = new URI(urlSource).toURL();
            } catch (URISyntaxException e) {
                throw new IOException("URL invalide : " + urlSource, e);
            }

            HttpURLConnection testConn = (HttpURLConnection) urlObj.openConnection();
            testConn.setRequestMethod("GET");
            testConn.setRequestProperty("Range", "bytes=0-1");
            testConn.setConnectTimeout(8000);
            testConn.setReadTimeout(8000);
            testConn.setRequestProperty("User-Agent", "Mozilla/5.0");

            int  code           = testConn.getResponseCode();
            boolean rangeSupporte = (code == HttpURLConnection.HTTP_PARTIAL);
            long tailleFichier  = -1;

            if (rangeSupporte) {
                String cr = testConn.getHeaderField("Content-Range");
                if (cr != null) {
                    int slash = cr.lastIndexOf('/');
                    if (slash >= 0) tailleFichier = Long.parseLong(cr.substring(slash + 1).trim());
                }
            } else {
                tailleFichier = testConn.getContentLengthLong();
            }
            testConn.disconnect();

            this.tailleTotaleMo = (tailleFichier > 0) ? tailleFichier / (1024.0 * 1024.0) : -1;

            File destDir = new File(cheminDestination);
            if (!destDir.exists()) destDir.mkdirs();

            // ── Choix du mode ────────────────────────────────────────────────
            if (rangeSupporte && tailleFichier > 0) {
                telechargerMultiSegments(tailleFichier);        // CORRECTION 1 + 2
            } else {
                telechargerMonoThread(tailleFichier);
            }

            if (annulee) return;

            // ── Fin ─ succès ─────────────────────────────────────────────────
            this.progression = 100.0;
            this.statut      = StatutTache.TERMINE;
            this.dateFin     = LocalDateTime.now();
            this.vitesseMoS  = 0.0;
            this.etaSecondes = -1;

            // CORRECTION 3 : calcul du hash SHA-256
            this.hashSha256 = calculerSha256(new File(cheminDestination, nomFichier));
            System.out.println("[INTÉGRITÉ] SHA-256 de " + nomFichier + " : " + hashSha256);

            if (gestionnaire != null && tailleTotaleMo > 0)
                gestionnaire.ajouterVolumeTelecharge(tailleTotaleMo);

            notifierTerminee();

        } catch (IOException e) {
            this.statut      = StatutTache.ERREUR;
            this.dateFin     = LocalDateTime.now();
            this.vitesseMoS  = 0.0;
            this.etaSecondes = -1;

            String msg = e.getMessage() != null ? e.getMessage() : "Erreur inconnue";
            System.err.println("[ERREUR] " + nomFichier + " : " + msg);
            notifierTerminee();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CORRECTION 1 + 2 : Téléchargement multi-segments avec REPRISE
    // ═════════════════════════════════════════════════════════════════════════
    private void telechargerMultiSegments(long tailleFichier) throws IOException {
        long tailleSegment = tailleFichier / NB_SEGMENTS;

        Thread[]        threads = new Thread[NB_SEGMENTS];
        SegmentWorker[] workers = new SegmentWorker[NB_SEGMENTS];
        long[]          segmentBytes = new long[NB_SEGMENTS];

        for (int i = 0; i < NB_SEGMENTS; i++) {
            long startByte = i * tailleSegment;
            long endByte   = (i == NB_SEGMENTS - 1) ? (tailleFichier - 1) : ((i + 1) * tailleSegment - 1);

            // ── CORRECTION 1 : détecter les fichiers .partX déjà existants ──
            File partFile   = new File(cheminDestination, nomFichier + ".part" + i);
            long dejaTeleg  = partFile.exists() ? partFile.length() : 0L;

            // On reprend depuis startByte + octets déjà téléchargés
            long repriseByte = startByte + dejaTeleg;

            if (dejaTeleg > 0) {
                System.out.println("[REPRISE] Segment " + i + " : " + dejaTeleg + " octets déjà présents, reprise depuis byte " + repriseByte);
                segmentBytes[i] = dejaTeleg;
            }

            workers[i] = new SegmentWorker(i, repriseByte, endByte, segmentBytes, tailleFichier, dejaTeleg > 0);
            threads[i] = new Thread(workers[i], "Segment-" + nomFichier + "-" + i);
            threads[i].start();
        }

        // Join de tous les segments
        boolean succes = true;
        for (int i = 0; i < NB_SEGMENTS; i++) {
            try {
                threads[i].join();
                if (workers[i].aEchoue()) succes = false;
            } catch (InterruptedException e) {
                succes = false;
                Thread.currentThread().interrupt();
            }
        }

        if (annulee) {
            changerStatutAnnule();
            return;
        }

        if (!succes) throw new IOException("Un ou plusieurs segments ont échoué.");

        // Fusion des segments
        fusionnerSegments();
        nettoyerFichiersSegments();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CORRECTION 3 : Calcul SHA-256 du fichier final
    // ═════════════════════════════════════════════════════════════════════════
    private String calculerSha256(File fichier) {
        if (!fichier.exists()) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream fis = new FileInputStream(fichier)) {
                byte[] buf = new byte[16384];
                int read;
                while ((read = fis.read(buf)) != -1) {
                    digest.update(buf, 0, read);
                }
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            System.err.println("[SHA256] Erreur calcul hash : " + e.getMessage());
            return null;
        }
    }

    // ─── Fusion des .partX → fichier final ───────────────────────────────────
    private void fusionnerSegments() throws IOException {
        File finalFile = new File(cheminDestination, nomFichier);
        try (FileOutputStream fos = new FileOutputStream(finalFile)) {
            byte[] buf = new byte[16384];
            for (int i = 0; i < NB_SEGMENTS; i++) {
                File part = new File(cheminDestination, nomFichier + ".part" + i);
                try (FileInputStream fis = new FileInputStream(part)) {
                    int read;
                    while ((read = fis.read(buf)) != -1) fos.write(buf, 0, read);
                }
            }
        }
        System.out.println("[FUSION] Fichier assemblé : " + nomFichier);
    }

    private void nettoyerFichiersSegments() {
        for (int i = 0; i < NB_SEGMENTS; i++) {
            File part = new File(cheminDestination, nomFichier + ".part" + i);
            if (part.exists()) part.delete();
        }
    }

    // ─── Mono-thread de secours ──────────────────────────────────────────────
    private void telechargerMonoThread(long tailleFichier) throws IOException {
        HttpURLConnection conn = null;
        InputStream       is   = null;
        FileOutputStream  fos  = null;

        try {
            URL urlObj = new URI(urlSource).toURL();
            conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            File destFile = new File(cheminDestination, nomFichier);
            is  = conn.getInputStream();
            fos = new FileOutputStream(destFile);

            byte[] buffer  = new byte[TAILLE_BLOC];
            int    read;
            long   bytesRead          = 0;
            long   lastNotif          = System.currentTimeMillis();
            long   lastSpeedUpdate    = System.currentTimeMillis();
            long   bytesAtLastSpeed   = 0;
            long   lastSauvegarde     = System.currentTimeMillis();

            while ((read = is.read(buffer)) != -1) {
                if (annulee) { changerStatutAnnule(); return; }
                fos.write(buffer, 0, read);
                bytesRead       += read;
                this.octetsRecus = bytesRead;

                this.progression = (tailleFichier > 0)
                        ? Math.min(100.0, bytesRead * 100.0 / tailleFichier)
                        : -1;

                long now = System.currentTimeMillis();
                majVitesse(now, lastSpeedUpdate, bytesRead, bytesAtLastSpeed, tailleFichier);
                if (now - lastSpeedUpdate >= 500) {
                    bytesAtLastSpeed = bytesRead;
                    lastSpeedUpdate  = now;
                }

                if (now - lastNotif >= INTERVALLE_NOTIFICATION_MS) {
                    notifierProgression();
                    lastNotif = now;
                }

                // Sauvegarde périodique des octets en BD pour reprise future
                if (gestionnaire != null && now - lastSauvegarde >= INTERVALLE_SAUVEGARDE_MS) {
                    gestionnaire.sauvegarderEnBD(this);
                    lastSauvegarde = now;
                }
            }
        } catch (URISyntaxException e) {
            throw new IOException("URL invalide", e);
        } finally {
            try { if (fos != null) fos.close(); } catch (IOException ignored) {}
            try { if (is  != null) is.close();  } catch (IOException ignored) {}
            if (conn != null) conn.disconnect();
        }
    }

    // ─── Calcul vitesse + ETA ─────────────────────────────────────────────────
    private void majVitesse(long now, long lastSpeedUpdate, long bytesRead,
                             long bytesAtLastSpeed, long tailleFichier) {
        long elapsed = now - lastSpeedUpdate;
        if (elapsed < 500) return;
        long   delta    = bytesRead - bytesAtLastSpeed;
        double speedMoS = (delta / (elapsed / 1000.0)) / (1024.0 * 1024.0);
        this.vitesseMoS = (this.vitesseMoS == 0.0)
                ? speedMoS
                : (0.3 * speedMoS + 0.7 * this.vitesseMoS);
        if (tailleFichier > 0 && this.vitesseMoS > 0)
            this.etaSecondes = (long) ((tailleFichier - bytesRead) / (this.vitesseMoS * 1024.0 * 1024.0));
        else
            this.etaSecondes = -1;
    }

    private void changerStatutAnnule() {
        this.statut      = StatutTache.ANNULE;
        this.dateFin     = LocalDateTime.now();
        this.vitesseMoS  = 0.0;
        this.etaSecondes = -1;
        notifierTerminee();
    }

    private int countActiveTasks() {
        if (gestionnaire == null) return 1;
        int active = 0;
        for (TacheTelechargement t : gestionnaire.lister())
            if (t.getStatut() == StatutTache.EN_COURS) active++;
        return Math.max(1, active);
    }

    private void notifierProgression() {
        if (listener != null) listener.onProgressionMiseAJour(this);
    }

    private void notifierTerminee() {
        if (listener != null) listener.onTacheTerminee(this);
    }

    public void annuler() { this.annulee = true; }
    public boolean estAnnulee() { return annulee; }

    public boolean verifierFichierValide() {
        File f = new File(cheminDestination, nomFichier);
        return f.exists() && f.length() > 0;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // INNER CLASS : SegmentWorker avec support de reprise (CORRECTION 1+2)
    // ═════════════════════════════════════════════════════════════════════════
    private class SegmentWorker implements Runnable {
        private final int     index;
        private final long    startByte;   // peut être > startOriginal si reprise
        private final long    endByte;
        private final long[]  segmentBytes;
        private final long    totalFichierSize;
        private final boolean modeReprise;
        private volatile boolean echec = false;

        SegmentWorker(int index, long startByte, long endByte,
                      long[] segmentBytes, long totalFichierSize, boolean modeReprise) {
            this.index           = index;
            this.startByte       = startByte;
            this.endByte         = endByte;
            this.segmentBytes    = segmentBytes;
            this.totalFichierSize = totalFichierSize;
            this.modeReprise     = modeReprise;
        }

        public boolean aEchoue() { return echec; }

        @Override
        public void run() {
            // Si segment déjà complet, ne rien faire
            if (startByte > endByte) {
                System.out.println("[SEGMENT " + index + "] Déjà complet, skip.");
                return;
            }

            HttpURLConnection conn = null;
            InputStream       is   = null;
            FileOutputStream  fos  = null;

            try {
                URL urlObj = new URI(urlSource).toURL();
                conn = (HttpURLConnection) urlObj.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Range", "bytes=" + startByte + "-" + endByte);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(30000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                int code = conn.getResponseCode();
                if (code != HttpURLConnection.HTTP_PARTIAL && code != HttpURLConnection.HTTP_OK)
                    throw new IOException("HTTP " + code + " pour segment " + index);

                File partFile = new File(cheminDestination, nomFichier + ".part" + index);
                is  = conn.getInputStream();
                // modeReprise = true → on AJOUTE à la fin du fichier existant
                fos = new FileOutputStream(partFile, modeReprise);

                byte[] buffer          = new byte[TAILLE_BLOC];
                int    read;
                long   lastNotif       = System.currentTimeMillis();
                long   lastSpeedUpd    = System.currentTimeMillis();
                long   bytesAtLastSpd  = 0;
                long   lastSauvegarde  = System.currentTimeMillis();

                while ((read = is.read(buffer)) != -1) {
                    if (annulee) return;
                    fos.write(buffer, 0, read);
                    segmentBytes[index] += read;

                    // Progression globale
                    long totalRecu = 0;
                    for (long b : segmentBytes) totalRecu += b;
                    octetsRecus = totalRecu;
                    progression = Math.min(99.9, totalRecu * 100.0 / totalFichierSize);

                    long now = System.currentTimeMillis();

                    // Vitesse + ETA pilotée par le segment 0
                    if (index == 0) {
                        majVitesse(now, lastSpeedUpd, totalRecu, bytesAtLastSpd, totalFichierSize);
                        if (now - lastSpeedUpd >= 500) {
                            bytesAtLastSpd = totalRecu;
                            lastSpeedUpd   = now;
                        }
                    }

                    if (now - lastNotif >= INTERVALLE_NOTIFICATION_MS) {
                        notifierProgression();
                        lastNotif = now;
                    }

                    // Sauvegarde périodique en BD (reprise future si crash)
                    if (gestionnaire != null && index == 0
                            && now - lastSauvegarde >= INTERVALLE_SAUVEGARDE_MS) {
                        gestionnaire.sauvegarderEnBD(TacheTelechargement.this);
                        lastSauvegarde = now;
                    }
                }

            } catch (Exception e) {
                echec = true;
                System.err.println("[SEGMENT " + index + "] Erreur : " + e.getMessage());
            } finally {
                try { if (fos  != null) fos.close();  } catch (IOException ignored) {}
                try { if (is   != null) is.close();   } catch (IOException ignored) {}
                if (conn != null) conn.disconnect();
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Factory depuisBD
    // ═════════════════════════════════════════════════════════════════════════
    public static TacheTelechargement depuisBD(
            String id, String nomFichier, String urlSource, String cheminDestination,
            double tailleMo, double progression, long octetsRecus,
            StatutTache statut, String hashSha256,
            LocalDateTime dateDebut, LocalDateTime dateFin) {

        TacheTelechargement t = new TacheTelechargement(nomFichier, urlSource, cheminDestination);
        try {
            java.lang.reflect.Field fId = TacheTelechargement.class.getDeclaredField("id");
            fId.setAccessible(true);
            fId.set(t, id);
        } catch (Exception e) {
            System.err.println("[TacheTelechargement] Impossible de restaurer l'id : " + e.getMessage());
        }
        t.tailleTotaleMo = tailleMo;
        t.progression    = progression;
        t.octetsRecus    = octetsRecus;
        t.statut         = statut;
        t.hashSha256     = hashSha256;
        t.dateDebut      = dateDebut;
        t.dateFin        = dateFin;
        return t;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Getters
    // ═════════════════════════════════════════════════════════════════════════
    public String        getId()                 { return id; }
    public String        getNomFichier()         { return nomFichier; }
    public double        getTailleTotaleMo()     { return tailleTotaleMo; }
    public double        getProgression()        { return progression; }
    public StatutTache   getStatut()             { return statut; }
    public long          getOctetsRecus()        { return octetsRecus; }
    public String        getUrlSource()          { return urlSource; }
    public double        getVitesseMoS()         { return vitesseMoS; }
    public long          getEtaSecondes()        { return etaSecondes; }
    public String        getCheminDestination()  { return cheminDestination; }
    public LocalDateTime getDateDebut()          { return dateDebut; }
    public LocalDateTime getDateFin()            { return dateFin; }
    public String        getHashSha256()         { return hashSha256; }
    public String        getHeureDebutFormatee() { return dateDebut != null ? dateDebut.format(FORMAT_HEURE) : "-"; }
    public String        getHeureFinFormatee()   { return dateFin   != null ? dateFin.format(FORMAT_HEURE)   : "-"; }

    @Override
    public String toString() {
        return nomFichier + " [" + statut + "] "
                + (progression >= 0 ? String.format("%.1f%%", progression) : "???");
    }
}
