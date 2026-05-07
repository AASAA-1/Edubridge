package com.example.edubridge.teacher;

import com.google.firebase.Timestamp;

public class BehaviorLogItem {

    private String id;
    private String studentId;
    private String studentName;
    private String teacherId;
    private String classId;
    private String className;
    private String subject;
    private String dayOfWeek;
    private String timeOfDay;
    private String incidentType;
    private String description;
    private Timestamp loggedAt;

    public BehaviorLogItem() {}

    public String getId()            { return id; }
    public void setId(String id)     { this.id = id; }

    public String getStudentId()                  { return studentId; }
    public void setStudentId(String studentId)     { this.studentId = studentId; }

    public String getStudentName()                { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getTeacherId()                  { return teacherId; }
    public void setTeacherId(String teacherId)     { this.teacherId = teacherId; }

    public String getClassId()                { return classId; }
    public void setClassId(String classId)     { this.classId = classId; }

    public String getClassName()                { return className; }
    public void setClassName(String className)   { this.className = className; }

    public String getSubject()               { return subject; }
    public void setSubject(String subject)    { this.subject = subject; }

    public String getDayOfWeek()                 { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek)    { this.dayOfWeek = dayOfWeek; }

    public String getTimeOfDay()                 { return timeOfDay; }
    public void setTimeOfDay(String timeOfDay)    { this.timeOfDay = timeOfDay; }

    public String getIncidentType()                    { return incidentType; }
    public void setIncidentType(String incidentType)    { this.incidentType = incidentType; }

    public String getDescription()                  { return description; }
    public void setDescription(String description)   { this.description = description; }

    public Timestamp getLoggedAt()               { return loggedAt; }
    public void setLoggedAt(Timestamp loggedAt)   { this.loggedAt = loggedAt; }
}
