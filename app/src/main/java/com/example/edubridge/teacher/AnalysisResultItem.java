package com.example.edubridge.teacher;

import com.google.firebase.Timestamp;

public class AnalysisResultItem {
    private String id;
    private String analysisText;
    private Timestamp analyzedAt;
    private boolean expanded = false;

    public AnalysisResultItem() {}

    public AnalysisResultItem(String id, String analysisText, Timestamp analyzedAt) {
        this.id           = id;
        this.analysisText = analysisText;
        this.analyzedAt   = analyzedAt;
    }

    public String getId()               { return id; }
    public String getAnalysisText()     { return analysisText; }
    public Timestamp getAnalyzedAt()    { return analyzedAt; }
    public boolean isExpanded()         { return expanded; }

    public void setId(String id)                    { this.id = id; }
    public void setAnalysisText(String analysisText){ this.analysisText = analysisText; }
    public void setAnalyzedAt(Timestamp analyzedAt) { this.analyzedAt = analyzedAt; }
    public void setExpanded(boolean expanded)       { this.expanded = expanded; }
}
