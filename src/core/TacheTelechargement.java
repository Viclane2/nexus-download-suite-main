package core;

import threading.MoteurTelechargement;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.net.URISyntaxException;  // <-- AJOUTE CETTE LIGNE

/**
 * Représente une tâche de téléchargement RÉEL unique.
 * Supporte le téléchargement parallèle multi-segment (4 segments) de type IDM
 * et la régulation de vitesse (throttling) en temps réel.
 */
public class TacheTelechargement implements Runnable, Serializable, ITask {

    private static final long serialVersionUID = 3L;
    private static final DateTimeFormatter FORMAT_HEURE = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int TAILLE_BLOC = 4096; // 4 Ko
    private static final long INTERVALLE_NOTIFICATION_MS = 120;
    private static final int NB_SEGMENTS = 4;

    private final String id;
    private final String nomFichier;
    private final String urlSource;
    private final String cheminDestination;
    private volatile double tailleTotaleMo;
    private volatile double progression;
    private volatile long octetsRecus;
    private volatile StatutTache statut;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private volatile double vitesseMoS = 0.0;
    private volatile long etaSecondes = -1;

    private transient ProgressionListener listener;
    private transient GestionnaireTaches gestionnaire;
    private transient volatile MoteurTelechargement moteur;
    private transient volatile boolean annulee = false;

    public TacheTelechargement(String nomFichier, String urlSource, String cheminDestination) {
        this.id = UUID.randomUUID().toString();
        this.nomFichier = nomFichier;
        this.urlSource = urlSource;
        this.cheminDestination = cheminDestination;
        this.tailleTotaleMo = -1;
        this.progression = 0.0;
        this.statut = StatutTache.EN_ATTENTE;
        this.octetsRecus = 0;
    }

    public void setListener(ProgressionListener listener) {
        this.listener = listener;
    }

    /**
     * Vérifie si le fichier téléchargé est un fichier valide (non corrompu).
     * Cette méthode vérifie simplement si le fichier existe et a une taille > 0.
     */
    public boolean verifierFichierValide() {
        File fichier = new File(cheminDestination, nomFichier);
        return fichier.exists() && fichier.length() > 0;
    }

    public void setGestionnaire(GestionnaireTaches gestionnaire) {
        this.gestionnaire = gestionnaire;
    }

    public void setMoteur(MoteurTelechargement moteur) {
        this.moteur = moteur;
    }

    @Override
    public void run() {
        this.statut = StatutTache.EN_COURS;
        this.dateDebut = LocalDateTime.now();
        notifierProgression();

        try {
            // 1. Handshake pour tester la taille et le support des en-têtes Range
            //URL urlObj = new URI(urlSource).toURL();

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

            int code = testConn.getResponseCode();
            boolean rangeSupporte = (code == HttpURLConnection.HTTP_PARTIAL);
            long tailleFichier = -1;

            if (rangeSupporte) {
                String rangeHeader = testConn.getHeaderField("Content-Range");
                if (rangeHeader != null) {
                    int slashIdx = rangeHeader.lastIndexOf('/');
                    if (slashIdx >= 0) {
                        tailleFichier = Long.parseLong(rangeHeader.substring(slashIdx + 1).trim());
                    }
                }
            } else {
                tailleFichier = testConn.getContentLengthLong();
            }
            testConn.disconnect();

            if (tailleFichier > 0) {
                this.tailleTotaleMo = tailleFichier / (1024.0 * 1024.0);
            } else {
                this.tailleTotaleMo = -1;
            }

            File destDir = new File(cheminDestination);
            if (!destDir.exists()) {
                destDir.mkdirs();
            }

            if (rangeSupporte && tailleFichier > 0) {
                // TÉLÉCHARGEMENT MULTI-SEGMENT PARALLÈLE (Style IDM)
                long tailleSegment = tailleFichier / NB_SEGMENTS;
                Thread[] threads = new Thread[NB_SEGMENTS];
                SegmentWorker[] workers = new SegmentWorker[NB_SEGMENTS];
                long[] segmentBytes = new long[NB_SEGMENTS];

                for (int i = 0; i < NB_SEGMENTS; i++) {
                    long start = i * tailleSegment;
                    long end = (i == NB_SEGMENTS - 1) ? (tailleFichier - 1) : ((i + 1) * tailleSegment - 1);
                    workers[i] = new SegmentWorker(i, start, end, segmentBytes, tailleFichier);
                    threads[i] = new Thread(workers[i], "Segment-" + nomFichier + "-" + i);
                    threads[i].start();
                }

                // Attendre tous les segments (Join)
                boolean succesSegments = true;
                for (int i = 0; i < NB_SEGMENTS; i++) {
                    try {
                        threads[i].join();
                        if (workers[i].aEchoue()) {
                            succesSegments = false;
                        }
                    } catch (InterruptedException e) {
                        succesSegments = false;
                        Thread.currentThread().interrupt();
                    }
                }

                if (annulee) {
                    changerStatutAnnule();
                    nettoyerFichiersSegments();
                    return;
                }

                if (!succesSegments) {
                    throw new IOException("Un ou plusieurs segments ont échoué.");
                }

                // Fusionner les segments (Style IDM Stitch)
                fusionnerSegments();
                nettoyerFichiersSegments();

            } else {
                // TÉLÉCHARGEMENT MONO-THREAD DE SECOURS
                telechargerMonoThread(tailleFichier);
            }

            // Téléchargement terminé avec succès
            this.progression = 100.0;
            this.statut = StatutTache.TERMINE;
            this.dateFin = LocalDateTime.now();
            this.vitesseMoS = 0.0;
            this.etaSecondes = -1;

            if (gestionnaire != null && tailleTotaleMo > 0) {
                gestionnaire.ajouterVolumeTelecharge(tailleTotaleMo);
            }
            notifierTerminee();

        } catch (IOException e) {
            this.statut = StatutTache.ERREUR;
            this.dateFin = LocalDateTime.now();
            
            // Message d'erreur plus explicite
            String erreurMsg;
            if (e.getMessage().contains("HTTP") && e.getMessage().contains("404")) {
                erreurMsg = "Fichier non trouvé (404) : vérifiez l'URL";
            } else if (e.getMessage().contains("HTTP") && e.getMessage().contains("403")) {
                erreurMsg = "Accès refusé (403) : fichier protégé";
            } else if (e.getMessage().contains("HTTP") && e.getMessage().contains("500")) {
                erreurMsg = "Erreur serveur (500) : réessayez plus tard";
            } else {
                erreurMsg = "Erreur de téléchargement : " + e.getMessage();
            }
            
            this.vitesseMoS = 0.0;
            this.etaSecondes = -1;
            
            // Nettoyer le fichier partiel
            File fichierPartiel = new File(cheminDestination, nomFichier);
            if (fichierPartiel.exists()) {
                fichierPartiel.delete();
            }
            
            notifierTerminee();
            // Log l'erreur
            System.err.println("[ERREUR] " + erreurMsg);
        }
    }

    private void changerStatutAnnule() {
        this.statut = StatutTache.ANNULE;
        this.dateFin = LocalDateTime.now();
        this.vitesseMoS = 0.0;
        this.etaSecondes = -1;
        notifierTerminee();
    }

    private void fusionnerSegments() throws IOException {
        File finalFile = new File(cheminDestination, nomFichier);
        try (FileOutputStream fos = new FileOutputStream(finalFile)) {
            byte[] buf = new byte[16384];
            for (int i = 0; i < NB_SEGMENTS; i++) {
                File partFile = new File(cheminDestination, nomFichier + ".part" + i);
                try (FileInputStream fis = new FileInputStream(partFile)) {
                    int read;
                    while ((read = fis.read(buf)) != -1) {
                        fos.write(buf, 0, read);
                    }
                }
            }
        }
    }

    private void nettoyerFichiersSegments() {
        for (int i = 0; i < NB_SEGMENTS; i++) {
            File partFile = new File(cheminDestination, nomFichier + ".part" + i);
            if (partFile.exists()) {
                partFile.delete();
            }
        }
    }

    private void telechargerMonoThread(long tailleFichier) throws IOException {
        HttpURLConnection conn = null;
        InputStream is = null;
        FileOutputStream fos = null;
        
        try {
            URL urlObj = new URI(urlSource).toURL();
            conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            File destFile = new File(cheminDestination, nomFichier);
            is = conn.getInputStream();
            fos = new FileOutputStream(destFile);

            byte[] buffer = new byte[TAILLE_BLOC];
            int read;
            long bytesRead = 0;
            long lastNotification = System.currentTimeMillis();
            long speedUpdateTime = System.currentTimeMillis();
            long bytesAtLastSpeedUpdate = 0;

            long bytesInLimiterInterval = 0;
            long limiterIntervalStart = System.currentTimeMillis();

            while ((read = is.read(buffer)) != -1) {
                if (annulee) {
                    changerStatutAnnule();
                    return;
                }

                fos.write(buffer, 0, read);
                bytesRead += read;
                this.octetsRecus = bytesRead;

                if (tailleFichier > 0) {
                    this.progression = Math.min(100.0, (bytesRead * 100.0) / tailleFichier);
                } else {
                    this.progression = -1;
                }

                // Régulation de vitesse (Throttling)
                bytesInLimiterInterval += read;
                if (moteur != null && moteur.isLimiteurVitesseActif()) {
                    double limitPerTask = (moteur.getLimiteVitesseKoS() * 1024.0) / Math.max(1, countActiveTasks());
                    long elapsed = System.currentTimeMillis() - limiterIntervalStart;
                    if (elapsed > 0) {
                        double currentSpeed = bytesInLimiterInterval / (elapsed / 1000.0);
                        if (currentSpeed > limitPerTask) {
                            long expectedTime = (long) (bytesInLimiterInterval * 1000.0 / limitPerTask);
                            long delay = expectedTime - elapsed;
                            if (delay > 0) {
                                try {
                                    Thread.sleep(delay);
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    return;
                                }
                            }
                        }
                    }
                }

                long now = System.currentTimeMillis();
                if (now - limiterIntervalStart >= 100) {
                    bytesInLimiterInterval = 0;
                    limiterIntervalStart = now;
                }

                // Calcul de la vitesse instantanée lissée
                long elapsedSpeed = now - speedUpdateTime;
                if (elapsedSpeed >= 500) {
                    long speedBytes = bytesRead - bytesAtLastSpeedUpdate;
                    double speedMoS = (speedBytes / (elapsedSpeed / 1000.0)) / (1024.0 * 1024.0);
                    this.vitesseMoS = (this.vitesseMoS == 0.0) ? speedMoS : (0.3 * speedMoS + 0.7 * this.vitesseMoS);
                    
                    if (tailleFichier > 0 && this.vitesseMoS > 0) {
                        this.etaSecondes = (long) ((tailleFichier - bytesRead) / (this.vitesseMoS * 1024.0 * 1024.0));
                    } else {
                        this.etaSecondes = -1;
                    }
                    bytesAtLastSpeedUpdate = bytesRead;
                    speedUpdateTime = now;
                }

                if (now - lastNotification >= INTERVALLE_NOTIFICATION_MS) {
                    notifierProgression();
                    lastNotification = now;
                }
            }
            
        } catch (IOException e) {
            throw e;
        } catch (URISyntaxException e) {
            throw new IOException("URL invalide : " + urlSource, e);
        } finally {
            // Fermeture propre des ressources
            try {
                if (fos != null) fos.close();
            } catch (IOException ignored) {}
            try {
                if (is != null) is.close();
            } catch (IOException ignored) {}
            if (conn != null) conn.disconnect();
        }
    }

    private int countActiveTasks() {
        if (gestionnaire == null) return 1;
        int active = 0;
        for (TacheTelechargement t : gestionnaire.lister()) {
            if (t.getStatut() == StatutTache.EN_COURS) {
                active++;
            }
        }
        return Math.max(1, active);
    }

    private void notifierProgression() {
        if (listener != null) {
            listener.onProgressionMiseAJour(this);
        }
    }

    private void notifierTerminee() {
        if (listener != null) {
            listener.onTacheTerminee(this);
        }
    }

    public void annuler() {
        this.annulee = true;
    }

    public boolean estAnnulee() {
        return annulee;
    }

    // --- Inner class SegmentWorker ---
    private class SegmentWorker implements Runnable {
        private final int index;
        private final long startByte;
        private final long endByte;
        private final long[] segmentBytes;
        private final long totalFichierSize;
        private volatile boolean echec = false;

        public SegmentWorker(int index, long startByte, long endByte, long[] segmentBytes, long totalFichierSize) {
            this.index = index;
            this.startByte = startByte;
            this.endByte = endByte;
            this.segmentBytes = segmentBytes;
            this.totalFichierSize = totalFichierSize;
        }

        public boolean aEchoue() {
            return echec;
        }

        @Override
        public void run() {
            HttpURLConnection conn = null;
            InputStream is = null;
            FileOutputStream fos = null;

            try {
                URL urlObj = new URI(urlSource).toURL();
                conn = (HttpURLConnection) urlObj.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Range", "bytes=" + startByte + "-" + endByte);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(30000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                int code = conn.getResponseCode();
                if (code != HttpURLConnection.HTTP_PARTIAL && code != HttpURLConnection.HTTP_OK) {
                    throw new IOException("HTTP code " + code + " pour segment " + index);
                }

                File partFile = new File(cheminDestination, nomFichier + ".part" + index);
                is = conn.getInputStream();
                fos = new FileOutputStream(partFile);

                byte[] buffer = new byte[TAILLE_BLOC];
                int read;
                long bytesRead = 0;
                long lastNotification = System.currentTimeMillis();
                long lastSpeedUpdateTime = System.currentTimeMillis();
                long bytesAtLastSpeedUpdate = 0;

                long bytesInLimiterInterval = 0;
                long limiterIntervalStart = System.currentTimeMillis();

                while ((read = is.read(buffer)) != -1) {
                    if (annulee) {
                        return;
                    }

                    fos.write(buffer, 0, read);
                    bytesRead += read;
                    segmentBytes[index] = bytesRead;

                    // Calcul de la progression globale
                    long totalRecu = 0;
                    for (int k = 0; k < NB_SEGMENTS; k++) {
                        totalRecu += segmentBytes[k];
                    }
                    octetsRecus = totalRecu;
                    progression = Math.min(99.9, (totalRecu * 100.0) / totalFichierSize);

                    // Throttling de vitesse segmentaire
                    bytesInLimiterInterval += read;
                    if (moteur != null && moteur.isLimiteurVitesseActif()) {
                        double limitPerSegment = (moteur.getLimiteVitesseKoS() * 1024.0) / (countActiveTasks() * NB_SEGMENTS);
                        long elapsed = System.currentTimeMillis() - limiterIntervalStart;
                        if (elapsed > 0) {
                            double currentSpeed = bytesInLimiterInterval / (elapsed / 1000.0);
                            if (currentSpeed > limitPerSegment) {
                                long expectedTime = (long) (bytesInLimiterInterval * 1000.0 / limitPerSegment);
                                long delay = expectedTime - elapsed;
                                if (delay > 0) {
                                    Thread.sleep(delay);
                                }
                            }
                        }
                    }

                    long now = System.currentTimeMillis();
                    if (now - limiterIntervalStart >= 100) {
                        bytesInLimiterInterval = 0;
                        limiterIntervalStart = now;
                    }

                    // Calcul vitesse globale lissée (piloté par le segment 0 pour éviter la duplication)
                    if (index == 0) {
                        long elapsedSpeed = now - lastSpeedUpdateTime;
                        if (elapsedSpeed >= 500) {
                            long totalBytesAtNow = octetsRecus;
                            // Approximer la vitesse
                            long speedBytes = totalBytesAtNow - bytesAtLastSpeedUpdate;
                            double speedMoS = (speedBytes / (elapsedSpeed / 1000.0)) / (1024.0 * 1024.0);
                            vitesseMoS = (vitesseMoS == 0.0) ? speedMoS : (0.3 * speedMoS + 0.7 * vitesseMoS);
                            
                            if (vitesseMoS > 0) {
                                etaSecondes = (long) ((totalFichierSize - totalBytesAtNow) / (vitesseMoS * 1024.0 * 1024.0));
                            }
                            bytesAtLastSpeedUpdate = totalBytesAtNow;
                            lastSpeedUpdateTime = now;
                        }
                    }

                    if (now - lastNotification >= INTERVALLE_NOTIFICATION_MS) {
                        notifierProgression();
                        lastNotification = now;
                    }
                }

                // SUPPRIME LE BLOC DE CODE CI-DESSUS QUI CAUSE L'ERREUR
                // Le bloc qui utilise "tailleFichier" a été déplacé dans la méthode run() principale

            } catch (Exception e) {
                echec = true;
            } finally {
                try {
                    if (fos != null) fos.close();
                } catch (IOException ignored) {}
                try {
                    if (is != null) is.close();
                } catch (IOException ignored) {}
                if (conn != null) conn.disconnect();
            }
        }
    }

    // --- Getters & Overrides ---
    public String getId() { return id; }
    public String getNomFichier() { return nomFichier; }
    public double getTailleTotaleMo() { return tailleTotaleMo; }
    public double getProgression() { return progression; }
    public StatutTache getStatut() { return statut; }
    public String getHeureDebutFormatee() { return dateDebut != null ? dateDebut.format(FORMAT_HEURE) : "-"; }
    public String getHeureFinFormatee() { return dateFin != null ? dateFin.format(FORMAT_HEURE) : "-"; }
    public long getOctetsRecus() { return octetsRecus; }
    public String getUrlSource() { return urlSource; }
    public double getVitesseMoS() { return vitesseMoS; }
    public long getEtaSecondes() { return etaSecondes; }

    @Override
    public String toString() {
        return nomFichier + " [" + statut + "] " + (progression >= 0 ? String.format("%.1f%%", progression) : "???");
    }
}