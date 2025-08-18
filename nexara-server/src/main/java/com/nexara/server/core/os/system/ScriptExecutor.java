package com.nexara.server.core.os.system;

import com.nexara.server.core.connect.ServerConnection;
import com.nexara.server.core.exception.connect.CommandExecutionException;
import com.nexara.server.polo.enums.ConnectErrorCode;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@RequiredArgsConstructor
public class ScriptExecutor {

    private final ServerConnection connection;

    public void runScript(String scriptPath) throws CommandExecutionException {
        try {
            List<String> lines = Files.readAllLines(Paths.get(scriptPath));
            String currentStep = null;

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                if (trimmed.startsWith("#")) {
                    // 注释行作为步骤说明
                    currentStep = trimmed.substring(1).trim();
                } else {
                    try {
                        connection.executeCommand(trimmed);
                    } catch (CommandExecutionException e) {
                        throw new CommandExecutionException(
                                ConnectErrorCode.COMMAND_EXECUTION_FAILED,
                                trimmed,
                                connection.getServerInfo().getServerId(),
                                "步骤 【" + currentStep + "】 出错"
                        );
                    }
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("无法读取脚本: " + scriptPath, e);
        }
    }
}