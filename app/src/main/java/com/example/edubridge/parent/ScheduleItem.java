package com.example.edubridge.parent;

public class ScheduleItem {
    public String day;
    public String start;
    public String end;
    public String subject;
    public String room;

    public ScheduleItem() {}

    public ScheduleItem(String day, String start, String end, String subject, String room) {
        this.day = day;
        this.start = start;
        this.end = end;
        this.subject = subject;
        this.room = room;
    }
}
