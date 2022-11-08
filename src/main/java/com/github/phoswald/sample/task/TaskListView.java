package com.github.phoswald.sample.task;

import java.util.List;

import com.github.phoswald.sample.utils.AbstractView;

public class TaskListView extends AbstractView<List<TaskViewModel>> {

    public TaskListView() {
        super("task-list", "tasks");
    }
}
