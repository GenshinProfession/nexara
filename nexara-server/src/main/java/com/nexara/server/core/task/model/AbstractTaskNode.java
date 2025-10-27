package com.nexara.server.core.task.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Getter
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = GroupTask.class, name = "group"),
        @JsonSubTypes.Type(value = LeafTask.class, name = "leaf")
})
public abstract class AbstractTaskNode implements TaskNode {

    private static final Logger log = LoggerFactory.getLogger(AbstractTaskNode.class);

    protected final String taskId;
    protected final String name;
    protected TaskStatus status;

    public AbstractTaskNode(String name) {
        this.taskId = UUID.randomUUID().toString();
        this.name = name;
        status = TaskStatus.PENDING;
    }

    public void setStatus(TaskStatus status) { this.status = status; }

    protected void updateStatus(TaskStatus newStatus) {
        this.status = newStatus;
        log.info("[{}] ({}) 状态变为: {}", name, taskId.substring(0, 8), newStatus);
    }
}