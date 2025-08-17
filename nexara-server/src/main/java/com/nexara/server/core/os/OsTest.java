package com.nexara.server.core.os;

import com.nexara.server.core.connect.ServerConnection;
import com.nexara.server.core.connect.SshConnection;
import com.nexara.server.core.os.system.OperatingSystem;
import com.nexara.server.polo.enums.ServiceType;
import com.nexara.server.polo.model.ServerInfo;

public class OsTest {
    public static void main(String[] args) throws Exception {
        ServerInfo serverInfo = ServerInfo.builder().serverId("my_server").host("117.72.177.83").port(22).username("root").password("Cyy110120").build();
        ServerConnection serverConnection = new SshConnection(serverInfo);
        OperatingSystem os = OSFactory.createOS(serverConnection);
        os.installService(ServiceType.NETCAT);
    }
}
