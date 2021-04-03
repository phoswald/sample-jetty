package com.github.phoswald.sample.task;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class TaskViewModel {

    public String taskId;
    public String timestamp;
    public String title;
    public String description;
    public boolean done;

    public TaskViewModel(TaskEntity entity) {
        this.taskId = entity.getTaskId();
        this.timestamp = format(entity.getTimestamp());
        this.title = entity.getTitle();
        this.description = entity.getDescription();
        this.done = entity.isDone();
    }

    public static List<TaskViewModel> newList(List<TaskEntity> entities) {
        return entities.stream().map(TaskViewModel::new).collect(Collectors.toList());
    }

    private String format(Instant instant) {

        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return dateTime.format(DateTimeFormatter.ISO_DATE) + " " + dateTime.format(DateTimeFormatter.ISO_TIME);
    }
}
