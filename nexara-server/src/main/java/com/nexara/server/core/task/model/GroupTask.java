package com.nexara.server.core.task.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.nexara.server.core.task.util.TaskContext;

import java.util.ArrayList;
import java.util.List;

public class GroupTask extends AbstractTaskNode {

    @JsonManagedReference
    private final List<TaskNode> children = new ArrayList<>();

    public GroupTask(String name) { super(name); }

    public void addChild(TaskNode node) { children.add(node); }

    @Override
    public void execute(TaskContext context) {
        updateStatus(TaskStatus.RUNNING);
        for (TaskNode child : children) {
            try {
                child.execute(context);
            } catch (Exception e) {
                updateStatus(TaskStatus.FAILED);
                throw e;
            }
        }
        updateStatus(TaskStatus.SUCCESS);
    }
}