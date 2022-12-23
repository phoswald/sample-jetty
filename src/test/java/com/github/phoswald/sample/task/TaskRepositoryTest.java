package com.github.phoswald.sample.task;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.phoswald.sample.ApplicationModule;

class TaskRepositoryTest {

    private static final ApplicationModule module = new ApplicationModule();

    @Test
    void testCrud() {
        try(TaskRepository testee = new TaskRepository(module.getEntityManagerFactory())) {
            assertEquals(0, testee.selectAllTasks().size());

            TaskEntity entity = new TaskEntity();
            entity.setNewTaskId();
            entity.setTitle("Test Title");
            entity.setDescription("Test Description");
            testee.createTask(entity);
        }

        try(TaskRepository testee = new TaskRepository(module.getEntityManagerFactory())) {
            List<TaskEntity> entites = testee.selectAllTasks();

            assertEquals(1, entites.size());
            assertEquals("Test Title", entites.get(0).getTitle());
            assertEquals("Test Description", entites.get(0).getDescription());
        }
    }
}
