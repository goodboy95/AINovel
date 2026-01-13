package com.ainovel.app.security.remote;

import fireflychat.user.v1.UserAuthServiceGrpc;
import fireflychat.user.v1.ValidateSessionRequest;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@ConditionalOnProperty(name = "sso.session-validation.enabled", havingValue = "true", matchIfMissing = true)
public class UserSessionValidator {

    @GrpcClient("user")
    private UserAuthServiceGrpc.UserAuthServiceBlockingStub userAuthStub;

    public boolean validate(long userId, String sessionId) {
        if (userId <= 0 || sessionId == null || sessionId.isBlank()) {
            return false;
        }
        return userAuthStub.validateSession(ValidateSessionRequest.newBuilder()
                .setUserId(userId)
                .setSessionId(sessionId)
                .build()).getValid();
    }
}
