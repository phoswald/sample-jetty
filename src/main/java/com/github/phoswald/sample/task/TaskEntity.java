package com.github.phoswald.sample.task;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "TASK")
@NamedQueries({ //
        @NamedQuery(name = TaskEntity.SELECT_ALL, query = "select t from TaskEntity t order by t.timestamp desc") })
public class TaskEntity {

    static final String SELECT_ALL = "TaskEntity.Select";

    @Id
    @Column(name = "TASK_ID")
    private String taskId;

    @Column(name = "USER_ID")
    private String userId;

    @Column(name = "TIMESTAMP")
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    @Column(name = "TITLE")
    private String title;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "DONE")
    private boolean done;

    public String getTaskId() {
        return taskId;
    }

    public void setNewTaskId() {
        this.taskId = UUID.randomUUID().toString();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Instant getTimestamp() {
        return timestamp == null ? null : timestamp.toInstant();
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp == null ? null : Date.from(timestamp);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}
