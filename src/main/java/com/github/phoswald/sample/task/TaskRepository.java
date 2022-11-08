package com.github.phoswald.sample.task;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;

public class TaskRepository implements AutoCloseable {

    private final EntityManager em;
    private boolean rollback;

    public TaskRepository(EntityManagerFactory emf) {
        em = emf.createEntityManager();
        em.getTransaction().begin();
    }

    @Override
    public void close() {
        try {
            if(rollback) {
                em.getTransaction().rollback();
            } else {
                em.getTransaction().commit();
            }
        } finally {
            em.close();
        }
    }

    public void setRollbackOnly() {
        rollback = true;
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
}
