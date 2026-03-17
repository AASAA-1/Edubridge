package com.example.edubridge.parent;

public class AttendanceRowItem {
    public String date;
    public String subject;
    public String status;

    public AttendanceRowItem() {}

    public AttendanceRowItem(String date, String subject, String status) {
        this.date = date;
        this.subject = subject;
        this.status = status;
    }
}