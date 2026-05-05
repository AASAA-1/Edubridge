package com.example.edubridge.parent;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.edubridge.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class StudentGameWebViewFragment extends Fragment {

    private WebView webView;
    private ProgressBar progressBar;

    private String gameId = "";
    private String gameTitle = "";
    private String gameUrl = "";
    private String studentId = "";

    private long startTimeMillis = 0;
    private boolean sessionSaved = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_game_webview, container, false);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            gameId = getArguments().getString("gameId", "");
            gameTitle = getArguments().getString("gameTitle", "");
            gameUrl = getArguments().getString("gameUrl", "");
            studentId = getArguments().getString("studentId", "");
        }

        startTimeMillis = System.currentTimeMillis();

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle(gameTitle);
        toolbar.setNavigationOnClickListener(v -> {
            saveSession();
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        progressBar = view.findViewById(R.id.progress_bar);
        webView = view.findViewById(R.id.game_webview);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setIndeterminate(false);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        if (!gameUrl.isEmpty()) {
            webView.loadUrl(gameUrl);
        }
    }

    @Override
    public void onDestroyView() {
        saveSession();
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroyView();
    }

    private void saveSession() {
        if (sessionSaved || startTimeMillis == 0) return;
        if (studentId.isEmpty() || gameId.isEmpty()) return;

        sessionSaved = true;

        long endTimeMillis = System.currentTimeMillis();
        long durationMs = endTimeMillis - startTimeMillis;
        double durationMinutes = durationMs / 60000.0;

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        Map<String, Object> session = new HashMap<>();
        session.put("studentId", studentId);
        session.put("gameId", gameId);
        session.put("gameTitle", gameTitle);
        session.put("startTime", new Date(startTimeMillis));
        session.put("endTime", new Date(endTimeMillis));
        session.put("durationMinutes", Math.round(durationMinutes * 10.0) / 10.0);
        session.put("userId", uid);

        FirebaseFirestore.getInstance()
                .collection("game_sessions")
                .add(session);
    }
}
