package com.ainovel.app.admin.dto;

public record SmtpStatusResponse(
        String host,
        Integer port,
        String username,
        boolean passwordIsSet
) {}

