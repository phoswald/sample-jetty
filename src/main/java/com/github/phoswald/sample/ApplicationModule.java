package com.github.phoswald.sample;

import java.util.HashMap;
import java.util.function.Supplier;

import com.github.phoswald.sample.sample.SampleController;
import com.github.phoswald.sample.sample.SampleResource;
import com.github.phoswald.sample.task.TaskController;
import com.github.phoswald.sample.task.TaskRepository;
import com.github.phoswald.sample.task.TaskResource;
import com.github.phoswald.sample.utils.ConfigProvider;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public interface ApplicationModule {

    default Application getApplication() {
        return new Application(getConfigProvider(), //
                getSampleResource(), getSampleController(), getTaskResource(), getTaskController());
    }

    default ConfigProvider getConfigProvider() {
        return new ConfigProvider();
    }

    default SampleResource getSampleResource() {
        return new SampleResource(getConfigProvider());
    }

    default SampleController getSampleController() {
        return new SampleController(getConfigProvider());
    }

    default TaskResource getTaskResource() {
        return new TaskResource(getTaskRepositoryFactory());
    }

    default TaskController getTaskController() {
        return new TaskController(getTaskRepositoryFactory());
    }

    default Supplier<TaskRepository> getTaskRepositoryFactory() {
        return () -> new TaskRepository(getEntityManagerFactory());
    }

    default EntityManagerFactory getEntityManagerFactory() {
        if (Global.emf == null) {
            var config = getConfigProvider();
            var props = new HashMap<>();
            config.getConfigProperty("app.jdbc.driver") //
                    .ifPresent(v -> props.put("jakarta.persistence.jdbc.driver", v));
            config.getConfigProperty("app.jdbc.url") //
                    .ifPresent(v -> props.put("jakarta.persistence.jdbc.url", v));
            config.getConfigProperty("app.jdbc.username") //
                    .ifPresent(v -> props.put("jakarta.persistence.jdbc.user", v));
            config.getConfigProperty("app.jdbc.password") //
                    .ifPresent(v -> props.put("jakarta.persistence.jdbc.password", v));
            Global.emf = Persistence.createEntityManagerFactory("taskDS", props);
        }
        return Global.emf;
    }

    public static class Global {

        private static EntityManagerFactory emf = null;
    }
}
