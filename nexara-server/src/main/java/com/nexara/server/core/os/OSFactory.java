package com.nexara.server.core.os;

import com.nexara.server.core.connect.ServerConnection;
import com.nexara.server.core.os.detection.OSDetector;
import com.nexara.server.core.os.system.OperatingSystem;
import com.nexara.server.core.os.system.centos.CentOS;
import com.nexara.server.core.os.system.ubuntu.UbuntuOS;
import com.nexara.server.core.os.system.windows.WindowsServerOS;
import com.nexara.server.polo.model.OSInfo;

public class OSFactory {
    public static OperatingSystem createOS(ServerConnection connection) throws Exception {
        OSInfo osInfo = OSDetector.detectOS(connection);
        Object var10000 = switch (osInfo.getName()) {
            case "Ubuntu" -> new UbuntuOS(connection);
            case "CentOS", "Red Hat Enterprise Linux" -> new CentOS(connection);
            case "Windows Server" -> new WindowsServerOS(connection);
            default -> throw new UnsupportedOperationException("Unsupported OS: " + String.valueOf(osInfo));
        };

        return (OperatingSystem)var10000;
    }
}
