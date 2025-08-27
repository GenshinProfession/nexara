package com.nexara.server.core.os.system.product;

import com.nexara.server.core.connect.product.ServerConnection;
import com.nexara.server.core.exception.connect.CommandExecutionException;
import com.nexara.server.core.os.ScriptExecutor;
import com.nexara.server.polo.enums.OSType;
import com.nexara.server.polo.enums.ServiceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.nio.file.Files;
import java.nio.file.Paths;

@RequiredArgsConstructor
@Log4j2
public class UbuntuOperatingSystem implements OperatingSystem {

    private final ServerConnection connection;

    public void installService(ServiceType serviceType) throws CommandExecutionException {
        String basePath = "scripts/ubuntu/" + serviceType.name().toLowerCase();
        ScriptExecutor executor = new ScriptExecutor(connection);

        String checkScript = basePath + "/check.cmds";
        if (Files.exists(Paths.get(checkScript))) {
            try {
                executor.runScript(checkScript);
                log.info("{} 已安装，跳过安装", serviceType);
                return;
            } catch (CommandExecutionException e) {
                log.info("{} 未安装，开始安装...", serviceType);
            }
        }

        String installScript = basePath + "/install.cmds";
        if (!Files.exists(Paths.get(installScript))) {
            throw new RuntimeException("安装脚本不存在: " + installScript);
        }

        executor.runScript(installScript);
        log.info("{} 安装完成", serviceType);
    }

    @Override
    public OSType getOSType() {
        return OSType.UBUNTU;
    }
}