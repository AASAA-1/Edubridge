package com.example.edubridge.teacher;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.edubridge.R;

import java.io.DataOutputStream;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;

public class TeacherLiveMonitoringFragment extends Fragment {

    private RtcEngine agoraEngine;
    private FrameLayout container;

    private final String APP_ID = "f2422e4b2f18499fb2ba6638d1e4ff7b";
    private final String CHANNEL_NAME = "class101";

    private Handler handler;

    // =========================
    // 🎯 AGORA EVENT HANDLER
    // =========================
    private final IRtcEngineEventHandler eventHandler = new IRtcEngineEventHandler() {

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            Log.d("AGORA", "Joined channel successfully");

            requireActivity().runOnUiThread(() -> startFrameCapture());
        }

        @Override
        public void onSnapshotTaken(int uid, String filePath, int width, int height, int errCode) {
            if (errCode == 0) {
                Log.d("SNAPSHOT", "✅ Snapshot ready: " + filePath);

                new Thread(() -> uploadImageFromPath(filePath)).start();

            } else {
                Log.e("SNAPSHOT", "❌ Snapshot failed, error code: " + errCode);
            }
        }
    };

    // =========================
    // 📱 LIFECYCLE
    // =========================
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_teacher_live_monitoring, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        container = view.findViewById(R.id.agoraContainer);
        handler = new Handler(Looper.getMainLooper());

        setupAgora();
    }

    // =========================
    // 📷 AGORA SETUP
    // =========================
    private void setupAgora() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = requireContext();
            config.mAppId = APP_ID;
            config.mEventHandler = eventHandler;

            agoraEngine = RtcEngine.create(config);

        } catch (Exception e) {
            Log.e("AGORA", "Error initializing Agora", e);
            return;
        }

        agoraEngine.enableVideo();

        agoraEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        agoraEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);

        SurfaceView surfaceView = RtcEngine.CreateRendererView(requireContext());
        container.addView(surfaceView);

        agoraEngine.setupLocalVideo(
                new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0)
        );

        ChannelMediaOptions options = new ChannelMediaOptions();
        options.publishCameraTrack = true;
        options.autoSubscribeVideo = true;
        options.autoSubscribeAudio = true;

        int result = agoraEngine.joinChannel(null, CHANNEL_NAME, 0, options);
        Log.d("AGORA", "Join result: " + result);
    }

    // =========================
    // 🔁 FRAME CAPTURE LOOP
    // =========================
    private void startFrameCapture() {
        Log.d("FRAME", "Starting frame capture loop...");

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                captureFrame();
                handler.postDelayed(this, 1000); // 1 FPS (safe)
            }
        }, 2000); // delay start
    }

    // =========================
    // 📸 TAKE SNAPSHOT
    // =========================
    private void captureFrame() {
        if (agoraEngine == null) {
            Log.e("FRAME", "Agora engine is null!");
            return;
        }

        String path = requireContext().getCacheDir() + "/frame.jpg";

        Log.d("FRAME", "Requesting snapshot...");
        agoraEngine.takeSnapshot(0, path);
    }

    // =========================
    // 🌐 UPLOAD IMAGE
    // =========================
    private void uploadImageFromPath(String path) {
        try {
            Log.d("UPLOAD", "Starting upload...");

            File file = new File(path);

            if (!file.exists()) {
                Log.e("UPLOAD", "❌ File does NOT exist!");
                return;
            }

            Log.d("UPLOAD", "File exists, size: " + file.length());

            byte[] bytes = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                bytes = Files.readAllBytes(file.toPath());
            }

            URL url = new URL("http://192.168.100.118:5000/analyze");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=boundary");

            DataOutputStream request = new DataOutputStream(conn.getOutputStream());

            request.writeBytes("--boundary\r\n");
            request.writeBytes("Content-Disposition: form-data; name=\"frame\"; filename=\"frame.jpg\"\r\n");
            request.writeBytes("Content-Type: image/jpeg\r\n\r\n");

            request.write(bytes);
            request.writeBytes("\r\n--boundary--\r\n");

            request.flush();
            request.close();

            int responseCode = conn.getResponseCode();
            Log.d("UPLOAD", "✅ Response code: " + responseCode);

        } catch (Exception e) {
            Log.e("UPLOAD", "Upload failed", e);
        }
    }

    // =========================
    // 🧹 CLEANUP
    // =========================
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        if (agoraEngine != null) {
            agoraEngine.leaveChannel();
            RtcEngine.destroy();
            agoraEngine = null;
        }
    }
}