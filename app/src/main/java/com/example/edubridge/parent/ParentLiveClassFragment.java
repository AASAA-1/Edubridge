package com.example.edubridge.parent;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.edubridge.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;

public class ParentLiveClassFragment extends Fragment {

    private RtcEngine agoraEngine;

    private final String APP_ID = "f2422e4b2f18499fb2ba6638d1e4ff7b";
    private final String CHANNEL_NAME = "class101";

    private FrameLayout container;

    private ListenerRegistration listener;
    private boolean fallHandled = false;

    private final IRtcEngineEventHandler mEventHandler = new IRtcEngineEventHandler() {

        @Override
        public void onUserJoined(int uid, int elapsed) {
            Log.d("AGORA", "Teacher joined: " + uid);

            requireActivity().runOnUiThread(() -> setupRemoteVideo(uid));
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_parent_live_class, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.container = view.findViewById(R.id.videoContainer);

        setupAgora();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        listener = db.collection("live_monitoring")
                .document("global")
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) return;

                    if (snapshot != null && snapshot.exists()) {
                        String status = snapshot.getString("status");

                        if ("fall_detected".equals(status) && !fallHandled) {
                            fallHandled = true;

                            Toast.makeText(getContext(), "⚠️ Fall detected!", Toast.LENGTH_LONG).show();

                            // Reset status to prevent spam
                            db.collection("live_monitoring")
                                    .document("global")
                                    .update("status", "idle");
                        }

                        // Reset flag when status goes back to idle
                        if ("idle".equals(status)) {
                            fallHandled = false;
                        }
                    }
                });
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

        // Viewer mode
        agoraEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        agoraEngine.setClientRole(Constants.CLIENT_ROLE_AUDIENCE);

        ChannelMediaOptions options = new ChannelMediaOptions();
        options.autoSubscribeVideo = true;
        options.autoSubscribeAudio = true;

        int result = agoraEngine.joinChannel(null, CHANNEL_NAME, 0, options);
        Log.d("AGORA", "Join result: " + result);
    }

    private void setupRemoteVideo(int uid) {
        SurfaceView surfaceView = RtcEngine.CreateRendererView(requireContext());

        container.removeAllViews();
        container.addView(surfaceView);

        agoraEngine.setupRemoteVideo(
                new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid)
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (listener != null) {
            listener.remove();
            listener = null;
        }
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