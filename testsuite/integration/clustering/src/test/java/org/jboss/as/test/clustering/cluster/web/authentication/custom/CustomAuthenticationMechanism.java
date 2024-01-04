/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web.authentication.custom;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.AuthenticationException;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.AutoApplySession;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.CredentialValidationResult.Status;
import jakarta.security.enterprise.identitystore.IdentityStoreHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Paul Ferraro
 */
@AutoApplySession
@ApplicationScoped
public class CustomAuthenticationMechanism implements HttpAuthenticationMechanism {

    public static final String USERNAME_HEADER = "X-USERNAME";
    public static final String PASSWORD_HEADER = "X-PASSWORD";
    public static final String MESSAGE_HEADER = "X-MESSAGE";
    public static final String INVALID_CREDENTIAL_MESSAGE = "Credential was not valid.";
    public static final String CREDENTIAL_REQUIRED_MESSAGE = "Credential is required.";

    @Inject
    private IdentityStoreHandler handler;

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response, HttpMessageContext context) throws AuthenticationException {
        String username = request.getHeader(USERNAME_HEADER);
        String password = request.getHeader(PASSWORD_HEADER);

        if ((username != null) && (password != null)) {
            Credential credential = new UsernamePasswordCredential(username, password);
            CredentialValidationResult result = this.handler.validate(credential);

            if (result.getStatus() == Status.VALID) {
                return context.notifyContainerAboutLogin(result);
            }

            response.addHeader(MESSAGE_HEADER, INVALID_CREDENTIAL_MESSAGE);
        } else {
            response.addHeader(MESSAGE_HEADER, CREDENTIAL_REQUIRED_MESSAGE);
        }

        return context.responseUnauthorized();
    }
}
