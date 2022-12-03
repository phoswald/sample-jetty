package com.github.phoswald.sample.task;

import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TaskController {

    private static final Logger logger = LogManager.getLogger();

    private final Supplier<TaskRepository> repositoryFactory;

    public TaskController(Supplier<TaskRepository> repositoryFactory) {
        this.repositoryFactory = repositoryFactory;
    }

    public String getTasksPage() {
        try(TaskRepository repository = repositoryFactory.get()) {
            List<TaskEntity> entities = repository.selectAllTasks();
            List<TaskViewModel> viewModel = TaskViewModel.newList(entities);
            return new TaskListView().render(viewModel);
        }
    }

    public String postTasksPage( //
            String title, //
            String description) {
        logger.info("Received from with title=" + title + ", description=" + description);
        try(TaskRepository repository = repositoryFactory.get()) {
            TaskEntity entity = new TaskEntity();
            entity.setNewTaskId();
            entity.setUserId("guest");
            entity.setTimestamp(Instant.now());
            entity.setTitle(title);
            entity.setDescription(description);
            entity.setDone(false);
            repository.createTask(entity);
        }
        return getTasksPage();
    }

    public String getTaskPage( //
            String id, //
            String action) {
        try(TaskRepository repository = repositoryFactory.get()) {
            TaskEntity entity = repository.selectTaskById(id);
            TaskViewModel viewModel = new TaskViewModel(entity);
            if (Objects.equals(action, "edit")) {
                return new TaskEditView().render(viewModel);
            } else {
                return new TaskView().render(viewModel);
            }
        }
    }

    public Object postTaskPage( //
            String id, //
            String action, //
            String title, //
            String description, //
            String done) {
        logger.info("Received from with id=" + id + ", action=" + action + ", title=" + title + ", description=" + description + ", done=" + done);
        try(TaskRepository repository = repositoryFactory.get()) {
            TaskEntity entity = repository.selectTaskById(id);
            if (Objects.equals(action, "delete")) {
                repository.deleteTask(entity);
                return Paths.get("/app/pages/tasks");
            }
            if (Objects.equals(action, "store")) {
                entity.setTimestamp(Instant.now());
                entity.setTitle(title);
                entity.setDescription(description);
                entity.setDone(Objects.equals(done, "on"));
                repository.updateChanges();
            }
        }
        return getTaskPage(id, null);
    }
}
