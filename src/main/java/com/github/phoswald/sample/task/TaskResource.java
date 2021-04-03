package com.github.phoswald.sample.task;

import java.time.Instant;
import java.util.List;

import com.github.phoswald.sample.task.TaskRepository.Transaction;

public class TaskResource {

    private final TaskRepository repository;

    public TaskResource(TaskRepository repository) {
        this.repository = repository;
    }

    public List<TaskEntity> getTasks() {
        try(Transaction txn = repository.openTransaction()) {
            List<TaskEntity> entities = repository.selectAllTasks();
            return entities;
        }
    }

    public TaskEntity postTasks(TaskEntity request) {
        try(Transaction txn = repository.openTransaction()) {
            TaskEntity entity = new TaskEntity();
            entity.setNewTaskId();
            entity.setUserId("guest");
            entity.setTimestamp(Instant.now());
            entity.setTitle(request.getTitle());
            entity.setDescription(request.getDescription());
            entity.setDone(request.isDone());
            repository.createTask(entity);
            return entity;
        }
    }

    public TaskEntity getTask(String id) {
        try(Transaction txn = repository.openTransaction()) {
            TaskEntity entity = repository.selectTaskById(id);
            return entity;
        }
    }

    public TaskEntity putTask(String id, TaskEntity request) {
        try(Transaction txn = repository.openTransaction()) {
            TaskEntity entity = repository.selectTaskById(id);
            entity.setTimestamp(Instant.now());
            entity.setTitle(request.getTitle());
            entity.setDescription(request.getDescription());
            entity.setDone(request.isDone());
            return entity;
        }
    }

    public Void deleteTask(String id) {
        try(Transaction txn = repository.openTransaction()) {
            TaskEntity entity = repository.selectTaskById(id);
            repository.deleteTask(entity);
            return null;
        }
    }
}
