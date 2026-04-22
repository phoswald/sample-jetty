package com.github.phoswald.sample.task;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "task_")
@NamedQueries({ //
        @NamedQuery(name = TaskEntity.SELECT_ALL, query = "select t from TaskEntity t order by t.timestamp desc") })
public class TaskEntity {

    static final String SELECT_ALL = "TaskEntity.Select";

    @Id
    @Column(name = "task_id_")
    private String taskId;

    @Column(name = "user_id_")
    private String userId;

    @Column(name = "timestamp_")
    private Instant timestamp;

    @Column(name = "title_")
    private String title;

    @Column(name = "description_")
    private String description;

    @Column(name = "done_")
    private Boolean done;

    public String getTaskId() {
        return taskId;
    }

    public void setNewTaskId() {
        this.taskId = UUID.randomUUID().toString().replace("-", "");
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
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
        return done == null ? false : done.booleanValue();
    }

    public void setDone(boolean done) {
        this.done = Boolean.valueOf(done);
    }
}
