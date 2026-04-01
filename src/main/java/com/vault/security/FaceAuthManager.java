package com.vault.security;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.file.*;

/**
 * FaceAuthManager — provides face-presence detection using OpenCV Haar Cascades.
 *
 * Security Model:
 *   This is a PRESENCE check, not an IDENTITY check. Any human face in frame
 *   will satisfy the requirement. This demonstrates image-processing integration
 *   with a security gate — appropriate for a v1.1 academic release.
 *
 * Implementation:
 *   - Loads haarcascade_frontalface_default.xml from the OpenCV native bundle.
 *   - Captures one frame from the default system webcam using JavaCV FrameGrabber.
 *   - Runs CascadeClassifier.detectMultiScale on the greyscale frame.
 *   - Returns true if at least one face rectangle is detected.
 *   - All resources (camera, classifier) are released after each call.
 */
public class FaceAuthManager {

    // Path to the Haar cascade file bundled with OpenCV
    private static final String CASCADE_RESOURCE = "/haarcascade_frontalface_default.xml";

    /**
     * Captures one webcam frame and returns true if a human face is detected.
     *
     * @return true if face detected, false otherwise (or on any error)
     */
    public boolean isFaceDetected() {
        File cascadeFile = null;
        try {
            // Extract the cascade XML from the classpath to a temp file
            cascadeFile = extractCascadeToTempFile();
            if (cascadeFile == null) {
                System.err.println("[FaceAuthManager] Could not load Haar cascade.");
                return false;
            }

            // Load the face classifier
            CascadeClassifier classifier = new CascadeClassifier(cascadeFile.getAbsolutePath());
            if (classifier.empty()) {
                System.err.println("[FaceAuthManager] CascadeClassifier failed to load.");
                return false;
            }

            // Grab one frame from the default webcam (device 0)
            try (FrameGrabber grabber = FrameGrabber.createDefault(0)) {
                grabber.start();
                Frame frame = grabber.grab();
                if (frame == null || frame.image == null) {
                    System.err.println("[FaceAuthManager] Could not grab frame from webcam.");
                    return false;
                }

                // Convert Frame → Mat (OpenCV format)
                OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
                Mat mat = converter.convert(frame);
                if (mat == null || mat.empty()) return false;

                // Detect faces
                RectVector faces = new RectVector();
                classifier.detectMultiScale(mat, faces);
                boolean detected = faces.size() > 0;
                System.out.println("[FaceAuthManager] Faces detected: " + faces.size());
                return detected;
            }

        } catch (Exception e) {
            System.err.println("[FaceAuthManager] Error during face detection: " + e.getMessage());
            return false;
        } finally {
            if (cascadeFile != null) cascadeFile.delete();
        }
    }

    // Extracts the Haar cascade XML from the classpath to a temp file (required for CascadeClassifier)
    private File extractCascadeToTempFile() {
        try (InputStream is = getClass().getResourceAsStream(CASCADE_RESOURCE)) {
            if (is == null) {
                // Fall back to the OpenCV bundled data path
                String opencvData = System.getProperty("user.home") + "/.m2/repository/org/bytedeco/opencv";
                System.err.println("[FaceAuthManager] Cascade not found on classpath; check: " + opencvData);
                return null;
            }
            File tempFile = File.createTempFile("haarcascade_frontalface_", ".xml");
            tempFile.deleteOnExit();
            Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return tempFile;
        } catch (Exception e) {
            System.err.println("[FaceAuthManager] Failed to extract cascade: " + e.getMessage());
            return null;
        }
    }
}
