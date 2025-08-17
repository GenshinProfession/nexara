package com.nexara.server.polo.model;

import com.nexara.server.polo.enums.ProtocolType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServerInfo {
    @NotBlank(message = "serverId cannot be blank")
    private String serverId;

    @NotBlank(message = "host cannot be blank")
    @Pattern(regexp = "^([a-zA-Z0-9-]+\\.)+[a-zA-Z0-9]{2,}$", message = "Invalid host format")
    private String host;

    private @NotNull(message = "port cannot be null") @Positive(message = "port must be a positive integer")
    Integer port;

    private @NotBlank(message = "username cannot be blank")
    String username;

    @NotBlank(message = "password cannot be blank")
    private String password;

    private String privateKey;
    private String passphrase;

    @NotNull(message = "protocol cannot be null")
    private ProtocolType protocol;

    private Integer isInitialized;
}
