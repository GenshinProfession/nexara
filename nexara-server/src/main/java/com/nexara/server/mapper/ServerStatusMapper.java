package com.nexara.server.mapper;

import com.nexara.server.polo.model.ServerStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface ServerStatusMapper {

    /**
     * 根据服务器ID查询服务器状态
     */
    ServerStatus selectByServerId(@Param("serverId") String serverId);

    /**
     * 查询所有服务器状态
     */
    List<ServerStatus> selectAll();

    /**
     * 插入服务器状态
     */
    int insert(ServerStatus serverStatus);

    /**
     * 更新服务器状态
     */
    int update(ServerStatus serverStatus);

    /**
     * 根据服务器ID删除服务器状态
     */
    int deleteByServerId(@Param("serverId") String serverId);

    /**
     * 根据负载状态查询服务器
     */
    List<ServerStatus> selectByLoadStatus(@Param("loadStatus") String loadStatus);

    /**
     * 根据网络状态查询服务器
     */
    List<ServerStatus> selectByNetworkStatus(@Param("networkStatus") String networkStatus);

    /**
     * 查询高负载服务器（负载为high或critical）
     */
    List<ServerStatus> selectHighLoadServers();

    /**
     * 查询磁盘使用率超过阈值的服务器
     */
    List<ServerStatus> selectByDiskUsageThreshold(@Param("threshold") Float threshold);

    /**
     * 查询内存使用率超过阈值的服务器
     */
    List<ServerStatus> selectByMemoryUsageThreshold(@Param("threshold") Float threshold);

    /**
     * 检查服务器ID是否存在
     */
    boolean existsByServerId(@Param("serverId") String serverId);

    /**
     * 删除旧数据
     */
    int deleteOldData(int i);
}