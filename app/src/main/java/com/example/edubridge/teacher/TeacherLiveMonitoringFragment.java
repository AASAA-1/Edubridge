package com.example.edubridge.teacher;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.edubridge.R;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;

public class TeacherLiveMonitoringFragment extends Fragment {

    private RtcEngine agoraEngine;

    private static final int PERMISSION_REQ_ID = 22;

    private final String APP_ID = "f2422e4b2f18499fb2ba6638d1e4ff7b";
    private final String CHANNEL_NAME = "class101";

    private FrameLayout container;

    private final IRtcEngineEventHandler mEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            Log.d("AGORA", "Joined channel successfully");
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_teacher_live_monitoring, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        container = view.findViewById(R.id.agoraContainer);

        if (checkPermissions()) {
            setupAgora();
        } else {
            requestPermissions();
        }
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(requireActivity(),
                new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                },
                PERMISSION_REQ_ID);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == PERMISSION_REQ_ID) {
            if (checkPermissions()) {
                setupAgora();
            }
        }
    }

    private void setupAgora() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = requireContext();
            config.mAppId = APP_ID;
            config.mEventHandler = mEventHandler;

            agoraEngine = RtcEngine.create(config);

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        agoraEngine.enableVideo();
        agoraEngine.enableLocalVideo(true);

        agoraEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        agoraEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);

        // ✅ FORCE BACK CAMERA HERE
        agoraEngine.setCameraCapturerConfiguration(
                new io.agora.rtc2.video.CameraCapturerConfiguration(
                        io.agora.rtc2.video.CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_REAR
                )
        );

        SurfaceView surfaceView = RtcEngine.CreateRendererView(requireContext());

        container.removeAllViews();
        container.addView(surfaceView);

        agoraEngine.setupLocalVideo(
                new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, -1)
        );

        agoraEngine.startPreview();

        ChannelMediaOptions options = new ChannelMediaOptions();
        options.publishCameraTrack = true;
        options.autoSubscribeVideo = true;
        options.autoSubscribeAudio = true;

        int result = agoraEngine.joinChannel(null, CHANNEL_NAME, 0, options);
        Log.d("AGORA", "Join result: " + result);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (agoraEngine != null) {
            agoraEngine.leaveChannel();
            RtcEngine.destroy();
            agoraEngine = null;
        }
    }
}