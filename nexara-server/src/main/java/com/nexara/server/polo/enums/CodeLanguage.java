package com.nexara.server.polo.enums;

import lombok.Getter;

@Getter
public enum CodeLanguage {
    JAVA("Java", "java", "JDK"),
    PYTHON("Python", "py", "Python"),
    NODE("Node.js", "js", "Node"),
    GOLANG("Go", "go", "Go"),
    UNKNOWN("Unknown", "", "");

    private final String displayName;
    private final String extension;
    private final String versionType;

    CodeLanguage(String displayName, String extension, String versionType) {
        this.displayName = displayName;
        this.extension = extension;
        this.versionType = versionType;
    }

    public static CodeLanguage fromExtension(String extension) {
        for (CodeLanguage language : values()) {
            if (language.getExtension().equalsIgnoreCase(extension)) {
                return language;
            }
        }
        return UNKNOWN;
    }
}