package com.nexara.server.mapper;

import com.nexara.server.polo.model.ServerInfo;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

@Repository
public interface ServerInfoMapper {
    @Select("SELECT server_id, host, port, username, password, private_key, passphrase, protocol,is_initialized FROM server_info WHERE server_id = #{serverId}")
    ServerInfo findByServerId(String var1);

    @Select("SELECT server_id, host, port, username, password, private_key, passphrase, protocol,is_initialized FROM server_info WHERE host = #{host}")
    ServerInfo findByHost(String var1);

    @Select("SELECT server_id, host, port, username, password, private_key, passphrase, protocol,is_initialized FROM server_info")
    List<ServerInfo> findAllServerInfo();

    @Delete("DELETE FROM server_info WHERE server_id = #{serverId}")
    void deleteByServerId(String var1);

    @Insert("INSERT INTO server_info (server_id, host, port, username, password, private_key, passphrase, protocol,is_initialized) VALUES (#{serverId}, #{host}, #{port}, #{username}, #{password}, #{privateKey}, #{passphrase}, #{protocol}, #{isInitialized})")
    @Options(useGeneratedKeys = true, keyProperty = "serverId")
    void save(ServerInfo var1);

    @Update("UPDATE server_info SET host = #{host}, port = #{port}, username = #{username}, password = #{password}, private_key = #{privateKey}, passphrase = #{passphrase}, protocol = #{protocol}, is_initialized = #{is_initialized} WHERE server_id = #{serverId}")
    @Options(useGeneratedKeys = true, keyProperty = "serverId")
    ServerInfo update(ServerInfo var1);
}
