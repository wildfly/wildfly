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
import java.security.Principal;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import jakarta.security.auth.message.AuthException;
import jakarta.security.auth.message.AuthStatus;
import jakarta.security.auth.message.MessageInfo;
import jakarta.security.auth.message.MessagePolicy;
import jakarta.security.auth.message.callback.CallerPrincipalCallback;
import jakarta.security.auth.message.callback.GroupPrincipalCallback;
import jakarta.security.auth.message.callback.PasswordValidationCallback;
import jakarta.security.auth.message.module.ServerAuthModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.wildfly.security.auth.principal.NamePrincipal;

/**
 * A simple {@link ServerAuthModule} implementation.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SimpleServerAuthModule implements ServerAuthModule {

    private static final String AUTH_TYPE = "jakarta.servlet.http.authType";
    private static final String SESSION = "jakarta.servlet.http.registerSession";
    static final String ANONYMOUS = "anonymous";

    private static final String AUTH_TYPE_HEADER = "X-AUTH-TYPE";
    private static final String USERNAME_HEADER = "X-USERNAME";
    private static final String PASSWORD_HEADER = "X-PASSWORD";
    private static final String ROLES_HEADER = "X-ROLES";
    private static final String MESSAGE_HEADER = "X-MESSAGE";
    private static final String SESSION_HEADER = "X-SESSION";

    private boolean selfValidating;
    private CallbackHandler callbackHandler;

    /**
     * Roles to be applied to every validated identity.
     */
    private String[] defaultRoles = null;

    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler, Map options)
            throws AuthException {
        this.callbackHandler = checkNotNullParam("handler", handler);
        this.selfValidating = "self-validating".equals(options.get("mode"));
        if (options.containsKey("default-roles")) {
            defaultRoles = String.valueOf(options.get("default-roles")).split(",");
        }
    }

    public Class[] getSupportedMessageTypes() {
        return new Class[] { HttpServletRequest.class, HttpServletResponse.class };
    }

    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) throws AuthException {
        HttpServletRequest request =  (HttpServletRequest) messageInfo.getRequestMessage();
        HttpServletResponse response = (HttpServletResponse) messageInfo.getResponseMessage();

        final String authType = request.getHeader(AUTH_TYPE_HEADER);
        final Principal currentPrincipal = request.getUserPrincipal();
        if (currentPrincipal != null && currentPrincipal.getName().equals("anonymous") == false) {
            handle(new CallerPrincipalCallback(clientSubject, currentPrincipal));
            if (authType != null) {
                messageInfo.getMap().put(AUTH_TYPE, authType);
            }
            return AuthStatus.SUCCESS;
        }

        final String username = request.getHeader(USERNAME_HEADER);
        final String password = request.getHeader(PASSWORD_HEADER);
        final String roles = request.getHeader(ROLES_HEADER);
        final String session = request.getHeader(SESSION_HEADER);

        if (username == null || username.length() == 0 || ((password == null || password.length() == 0) && !ANONYMOUS.equals(username))) {
            sendChallenge(response);
            return AuthStatus.SEND_CONTINUE;
        }

        final boolean validated;

        if ("anonymous".equals(username)) {
            validated = true; // Skip Authentication.
        } else if (selfValidating) {
            // In this mode the ServerAuthModule is taking over it's own validation and only using Callbacks to establish the identity.
            validated = "user1".equals(username) && "password1".equals(password);
        } else {
            PasswordValidationCallback pvc = new PasswordValidationCallback(serviceSubject, username, password.toCharArray());
            try {
                handle(pvc);
            } finally {
                pvc.clearPassword();
            }
            validated = pvc.getResult();
        }

        if (validated) {
            if ("anonymous".equals(username)) {
                handle(new CallerPrincipalCallback(clientSubject, (Principal) null));
            } else {
                handle(new CallerPrincipalCallback(clientSubject, new NamePrincipal(username)));
            }

            if (roles != null) {
                handle(new GroupPrincipalCallback(clientSubject, roles.split(",")));
            }
            if (defaultRoles != null) {
                handle(new GroupPrincipalCallback(clientSubject, defaultRoles));
            }

            Map map = messageInfo.getMap();
            if (authType != null) {
                map.put(AUTH_TYPE, authType);
            }

            if ("register".equals(session)) {
                System.out.println("Requesting session registration");
                map.put(SESSION, Boolean.TRUE.toString());
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
        return AuthStatus.SEND_SUCCESS;
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
