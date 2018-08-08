/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.integration.elytron.jaspi;

import static org.wildfly.common.Assert.checkNotNullParam;

import java.io.IOException;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.callback.GroupPrincipalCallback;
import javax.security.auth.message.callback.PasswordValidationCallback;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.wildfly.security.auth.principal.NamePrincipal;

/**
 * A simple {@link ServerAuthModule} implementation.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SimpleServerAuthModule implements ServerAuthModule {

    private static final String USERNAME_HEADER = "X-USERNAME";
    private static final String PASSWORD_HEADER = "X-PASSWORD";
    private static final String ROLES_HEADER = "X-ROLES";
    private static final String MESSAGE_HEADER = "X-MESSAGE";

    private CallbackHandler callbackHandler;

    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler, Map options)
            throws AuthException {
        this.callbackHandler = checkNotNullParam("handler", handler);
    }

    public Class[] getSupportedMessageTypes() {
        return new Class[] { HttpServletRequest.class, HttpServletResponse.class };
    }

    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) throws AuthException {
        HttpServletRequest request =  (HttpServletRequest) messageInfo.getRequestMessage();
        HttpServletResponse response = (HttpServletResponse) messageInfo.getResponseMessage();

        final String username = request.getHeader(USERNAME_HEADER);
        final String password = request.getHeader(PASSWORD_HEADER);
        final String roles = request.getHeader(ROLES_HEADER);

        if (username == null || username.length() == 0 || password == null || password.length() == 0) {
            sendChallenge(response);
            return AuthStatus.SEND_CONTINUE;
        }

        PasswordValidationCallback pvc = new PasswordValidationCallback(serviceSubject, username, password.toCharArray());
        try {
            handle(pvc);
        } finally {
            pvc.clearPassword();
        }
        if (pvc.getResult()) {
            Callback callerPrincipalCallback = new CallerPrincipalCallback(clientSubject, new NamePrincipal(username));
            if (roles != null) {
                handle(callerPrincipalCallback, new GroupPrincipalCallback(clientSubject, roles.split(",")));
            } else {
                handle(callerPrincipalCallback);
            }

            return AuthStatus.SUCCESS;
        } else {
            // It is a failure as authentication was deliberately attempted and the supplied username / password failed validation.
            sendChallenge(response);
            return AuthStatus.SEND_FAILURE;
        }

    }

    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {}

    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
        return AuthStatus.SUCCESS;
    }

    private void handle(final Callback... callbacks) throws AuthException {
        try {
            callbackHandler.handle(callbacks);
        } catch (IOException | UnsupportedCallbackException e) {
            e.printStackTrace();
            throw new AuthException(e.getMessage());
        }
    }

    private static void sendChallenge(HttpServletResponse response) {
        response.addHeader(MESSAGE_HEADER, "Please resubmit the request with a username specified using the X-USERNAME and a password specified using the X-PASSWORD header.");
        response.setStatus(401);
    }


}
