package com.vault.security;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * FaceAuthManager — provides two-mode key locking.
 *
 * MODE 1 — Face Identity Lock (when webcam is available):
 *   ENROLL:  Capture webcam frame → detect face → crop face ROI → save as JPEG to
 *            ~/.envvault/faces/<entryId>.jpg
 *   VERIFY:  Capture new frame → detect face → crop ROI → compare pixel MSE against
 *            saved image. Returns true if MSE < threshold (same person).
 *
 * MODE 2 — Grid PIN Lock (no webcam / user preference):
 *   A PIN string selected character-by-character from a shuffled alphanumeric grid.
 *   The PIN is stored as SHA-256 hash so the plaintext is never persisted.
 *   Use hashPin() to store and verifyPin() to check.
 */
public class FaceAuthManager {

    private static final String CASCADE_RESOURCE = "/haarcascade_frontalface_default.xml";
    private static final String FACES_DIR =
            System.getProperty("user.home") + File.separator + ".envvault" + File.separator + "faces";

    // MSE pixel threshold below which two face images are considered same identity
    private static final double SIMILARITY_THRESHOLD = 3000.0;

    // ─────────────────────────────────────────────────────────────
    //  Webcam Availability
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns true if a webcam (device 0) can be opened successfully.
     */
    public boolean isWebcamAvailable() {
        try (FrameGrabber grabber = FrameGrabber.createDefault(0)) {
            grabber.start();
            Frame f = grabber.grab();
            return f != null && f.image != null;
        } catch (Exception e) {
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  FACE MODE
    // ─────────────────────────────────────────────────────────────

    /**
     * Enrols the user's face by capturing a frame, detecting the largest face,
     * and saving the cropped greyscale face region to disk.
     *
     * @param entryId the vault entry ID (used as filename)
     * @return absolute path to the saved face image, or null on failure
     */
    public String enrollFace(int entryId) {
        try {
            Mat face = captureFaceMat();
            if (face == null) return null;

            // Ensure storage directory exists
            new File(FACES_DIR).mkdirs();
            String savePath = FACES_DIR + File.separator + "face_" + entryId + ".jpg";
            opencv_imgcodecs.imwrite(savePath, face);
            System.out.println("[FaceAuthManager] Face enrolled at: " + savePath);
            return savePath;
        } catch (Exception e) {
            System.err.println("[FaceAuthManager] Enrolment failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Verifies the current webcam face against the stored face image.
     *
     * @param savedFacePath path returned by enrollFace()
     * @return true if the faces match within the similarity threshold
     */
    public boolean verifyFace(String savedFacePath) {
        if (savedFacePath == null || !new File(savedFacePath).exists()) return false;
        try {
            Mat liveFace = captureFaceMat();
            if (liveFace == null) return false;

            Mat storedFace = opencv_imgcodecs.imread(savedFacePath, opencv_imgcodecs.IMREAD_GRAYSCALE);
            if (storedFace.empty()) return false;

            // Resize live face to match stored face dimensions for pixel comparison
            Mat resized = new Mat();
            opencv_imgproc.resize(liveFace, resized,
                    new Size(storedFace.cols(), storedFace.rows()));

            double mse = computeMSE(resized, storedFace);
            System.out.printf("[FaceAuthManager] MSE = %.1f (threshold = %.1f)%n",
                    mse, SIMILARITY_THRESHOLD);
            return mse < SIMILARITY_THRESHOLD;
        } catch (Exception e) {
            System.err.println("[FaceAuthManager] Verification failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Captures one webcam frame, detects the largest face, and returns the
     * greyscale cropped face region as a Mat. Returns null if no face found.
     */
    private Mat captureFaceMat() throws Exception {
        File cascadeFile = extractCascadeToTempFile();
        if (cascadeFile == null) return null;

        CascadeClassifier classifier = new CascadeClassifier(cascadeFile.getAbsolutePath());
        if (classifier.empty()) return null;

        try (FrameGrabber grabber = FrameGrabber.createDefault(0)) {
            grabber.start();
            Frame frame = grabber.grab();
            if (frame == null || frame.image == null) return null;

            OpenCVFrameConverter.ToMat conv = new OpenCVFrameConverter.ToMat();
            Mat colorMat = conv.convert(frame);
            Mat grayMat = new Mat();
            opencv_imgproc.cvtColor(colorMat, grayMat, opencv_imgproc.COLOR_BGR2GRAY);

            RectVector faces = new RectVector();
            classifier.detectMultiScale(grayMat, faces);
            if (faces.size() == 0) {
                System.out.println("[FaceAuthManager] No face detected in frame.");
                return null;
            }

            // Use the first (largest) detected face
            Rect faceRect = faces.get(0);
            return new Mat(grayMat, faceRect);
        } finally {
            cascadeFile.delete();
        }
    }

    // Computes Mean Squared Error between two same-size Mats
    private double computeMSE(Mat a, Mat b) {
        Mat diff = new Mat();
        org.bytedeco.opencv.global.opencv_core.absdiff(a, b, diff);
        diff.convertTo(diff, org.bytedeco.opencv.global.opencv_core.CV_32F);
        org.bytedeco.opencv.global.opencv_core.multiply(diff, diff, diff);
        Scalar sum = org.bytedeco.opencv.global.opencv_core.sumElems(diff);
        return sum.get(0) / (a.rows() * a.cols());
    }

    // ─────────────────────────────────────────────────────────────
    //  GRID PIN MODE
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns a SHA-256 hex hash of the given PIN string for secure storage.
     */
    public String hashPin(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(pin.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }

    /**
     * Returns true if the given raw PIN matches the stored hash.
     */
    public boolean verifyPin(String rawPin, String storedHash) {
        return hashPin(rawPin).equals(storedHash);
    }

    // ─────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────

    private File extractCascadeToTempFile() {
        try (InputStream is = getClass().getResourceAsStream(CASCADE_RESOURCE)) {
            if (is == null) return null;
            File tempFile = File.createTempFile("haarcascade_", ".xml");
            tempFile.deleteOnExit();
            Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return tempFile;
        } catch (Exception e) {
            System.err.println("[FaceAuthManager] Cascade extraction failed: " + e.getMessage());
            return null;
        }
    }
}
