//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.nexara.server.core.os.detection;

import com.nexara.server.core.exception.connect.CommandExecutionException;
import com.nexara.server.polo.enums.ConnectErrorCode;
import com.nexara.server.polo.model.OSInfo;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinuxDetectionStrategy implements OSDetectionStrategy {
    public String getDetectionCommand() {
        return "cat /etc/os-release || cat /etc/*-release || uname -a";
    }

    public OSInfo parseOutput(String output, String serverId) throws CommandExecutionException {
        String normalizedOutput = output.replace("\r\n", "\n");
        Matcher ubuntuMatcher = Pattern.compile("^ID=ubuntu$.*^VERSION_ID=\"?(\\d+\\.\\d+)\"?$", 40).matcher(normalizedOutput);
        if (ubuntuMatcher.find()) {
            return new OSInfo("Ubuntu", ubuntuMatcher.group(1));
        } else {
            Matcher centosMatcher = Pattern.compile("^ID=\"?(centos|rhel)\"?$.*^VERSION_ID=\"?(\\d+)\"?$", 42).matcher(normalizedOutput);
            if (centosMatcher.find()) {
                return new OSInfo("CentOS", centosMatcher.group(2));
            } else {
                Matcher prettyNameMatcher = Pattern.compile("PRETTY_NAME=\"?(.+?)\\s*(\\d+\\.\\d+).*?\"?", 2).matcher(normalizedOutput);
                if (prettyNameMatcher.find()) {
                    return new OSInfo(prettyNameMatcher.group(1), prettyNameMatcher.group(2));
                } else {
                    throw new CommandExecutionException(ConnectErrorCode.UNSUPPORTED_OPERATION, "os-detection", serverId, "无法探测到的操作系统. 原始输出:\n" + output);
                }
            }
        }
    }
}
