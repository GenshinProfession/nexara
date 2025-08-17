package com.nexara.server.core.os.system.ubuntu;

import com.nexara.server.core.connect.ServerConnection;
import com.nexara.server.core.exception.connect.CommandExecutionException;
import com.nexara.server.core.os.system.OperatingSystem;
import com.nexara.server.core.os.system.ScriptExecutor;
import com.nexara.server.polo.enums.ServiceType;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Log4j2
public class UbuntuOS implements OperatingSystem {

    private final ServerConnection connection;

    public void installService(ServiceType serviceType) throws CommandExecutionException {
        String basePath = "scripts/ubuntu/" + serviceType.name().toLowerCase();
        ScriptExecutor executor = new ScriptExecutor(this.connection);
        String checkScript = basePath + "/check.sh";
        if (Files.exists(Paths.get(checkScript), new LinkOption[0])) {
            try {
                executor.runScript(checkScript);
                log.info("{} 已安装，跳过安装", serviceType);
                return;
            } catch (CommandExecutionException var6) {
                log.info("{} 未安装，开始安装...", serviceType);
            }
        }

        String installScript = basePath + "/install.sh";
        if (!Files.exists(Paths.get(installScript), new LinkOption[0])) {
            throw new RuntimeException("安装脚本不存在: " + installScript);
        } else {
            executor.runScript(installScript);
            log.info("{} 安装完成", serviceType);
        }
    }
}
