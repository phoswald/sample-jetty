package com.github.phoswald.sample.task;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

public class TaskRepository implements AutoCloseable {

    private final EntityManager em;

    public TaskRepository(EntityManager em) {
        this.em = em;
    }

    public Transaction openTransaction() {
        em.getTransaction().begin();
        return new Transaction() {
            @Override
            public void close() {
                em.getTransaction().commit();
            }
        };
    }

    @Override
    public void close() {
        em.close();
    }

    public List<TaskEntity> selectAllTasks() {
        TypedQuery<TaskEntity> query = em.createNamedQuery(TaskEntity.SELECT_ALL, TaskEntity.class);
        query.setMaxResults(100);
        return query.getResultList();
    }

    public TaskEntity selectTaskById(String taskId) {
        return em.find(TaskEntity.class, taskId);
    }

    public void createTask(TaskEntity entity) {
        em.persist(entity);
    }

    public void deleteTask(TaskEntity entity) {
        em.remove(entity);
    }

    public void updateChanges() {
        em.flush();
    }

    public static interface Transaction extends AutoCloseable {
        @Override
        public void close();
    }
}
