package com.nexara.server.polo.model;



import java.io.Serializable;
import java.util.Date;

import com.nexara.server.polo.enums.ServerStatusEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
* 
* @TableName server_status
*/
@Data
public class ServerStatus implements Serializable {

    /**
    * 
    */
    @Size(max= -1,message="编码长度不能超过-1")
    @Length(max= -1,message="编码长度不能超过-1")
    private String serverId;
    /**
    * 
    */
    @NotNull(message="[]不能为空")
    private Integer cpuCores;
    /**
    * 
    */
    @NotNull(message="[]不能为空")
    private Float memorySizeGb;
    /**
    * 
    */
    @NotNull(message="[]不能为空")
    private Float memoryUsagePercent;
    /**
    * 
    */
    @NotNull(message="[]不能为空")
    private Float diskSizeGb;
    /**
    * 
    */
    @NotNull(message="[]不能为空")
    private Float diskUsagePercent;
    /**
    * 
    */
    @NotBlank(message="[]不能为空")
    @Size(max= -1,message="编码长度不能超过-1")
    @Length(max= -1,message="编码长度不能超过-1")
    private ServerStatusEnum networkStatus;
    /**
    * 
    */
    @NotBlank(message="[]不能为空")
    @Size(max= -1,message="编码长度不能超过-1")
    @Length(max= -1,message="编码长度不能超过-1")
    private ServerStatusEnum loadStatus;
    /**
    * 
    */
    private Date lastUpdated;
    /**
     * 错误信息
     */
    private String error;

}
