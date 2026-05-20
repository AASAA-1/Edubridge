package com.example.edubridge.shared.messaging;

import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;

public class VoiceRecorderHelper {

    // Audio source constants
    private static final int AUDIO_SOURCE_MIC = 1; // MediaRecorder.AudioSource.MIC
    private static final int AUDIO_SOURCE_VOICE_COMMUNICATION = 7; // For VoIP quality

    // Output format constants
    private static final int OUTPUT_FORMAT_MPEG_4 = 2; // MediaRecorder.OutputFormat.MPEG_4
    private static final int OUTPUT_FORMAT_THREE_GPP = 1; // More compatible

    // Audio encoder constants
    private static final int AUDIO_ENCODER_AAC = 3; // MediaRecorder.AudioEncoder.AAC
    private static final int AUDIO_ENCODER_AMR_NB = 1; // More compatible, smaller size

    private MediaRecorder recorder;
    private File outputFile;
    private boolean isRecording = false;
    private long startTime;
    private RecordingCallback callback;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable durationUpdater;

    public interface RecordingCallback {
        void onRecordingStarted();
        void onDurationChanged(int seconds);
        void onRecordingStopped(File audioFile, int duration);
        void onError(String error);
    }

    public void startRecording(File outputDirectory, RecordingCallback callback) {
        this.callback = callback;

        try {
            // Create temp file with appropriate extension
            String extension = ".mp4"; // AAC audio in MP4 container
            outputFile = File.createTempFile("voice_", extension, outputDirectory);

            recorder = new MediaRecorder();

            // Configure recorder with constants that work across API levels
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

            // Quality settings
            recorder.setAudioSamplingRate(22050); // Reduced for smaller file size
            recorder.setAudioEncodingBitRate(32000); // 32kbps is good for voice
            recorder.setAudioChannels(1); // Mono for voice

            recorder.setOutputFile(outputFile.getAbsolutePath());

            recorder.prepare();
            recorder.start();

            isRecording = true;
            startTime = System.currentTimeMillis();
            callback.onRecordingStarted();

            // Update duration every second
            durationUpdater = new Runnable() {
                @Override
                public void run() {
                    if (isRecording) {
                        int duration = (int) ((System.currentTimeMillis() - startTime) / 1000);
                        callback.onDurationChanged(duration);
                        handler.postDelayed(this, 1000);
                    }
                }
            };
            handler.post(durationUpdater);

        } catch (IOException e) {
            callback.onError("Failed to start recording: " + e.getMessage());
            cleanupTempFile();
        } catch (IllegalStateException e) {
            callback.onError("Recorder initialization failed: " + e.getMessage());
            cleanupTempFile();
        } catch (SecurityException e) {
            callback.onError("Microphone permission denied");
            cleanupTempFile();
        }
    }

    public void stopRecording() {
        if (recorder != null && isRecording) {
            try {
                recorder.stop();
                int duration = (int) ((System.currentTimeMillis() - startTime) / 1000);

                if (callback != null) {
                    // Only report if recording was at least 1 second
                    if (duration >= 1) {
                        callback.onRecordingStopped(outputFile, duration);
                    } else {
                        callback.onError("Recording too short");
                        cleanupTempFile();
                    }
                }
            } catch (RuntimeException e) {
                if (callback != null) {
                    callback.onError("Error stopping recording");
                }
                cleanupTempFile();
            } finally {
                releaseRecorder();
            }
        }
    }

    private void releaseRecorder() {
        try {
            if (recorder != null) {
                recorder.release();
            }
        } catch (RuntimeException e) {
            // Ignore errors during release
        } finally {
            recorder = null;
            isRecording = false;
            if (handler != null && durationUpdater != null) {
                handler.removeCallbacks(durationUpdater);
            }
        }
    }

    private void cleanupTempFile() {
        if (outputFile != null && outputFile.exists()) {
            outputFile.delete();
        }
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void cleanup() {
        if (isRecording) {
            stopRecording();
        }
        releaseRecorder();
        cleanupTempFile();
    }
}