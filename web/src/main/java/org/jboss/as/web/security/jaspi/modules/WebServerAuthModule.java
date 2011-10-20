/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.web.security.jaspi.modules;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.jboss.as.web.security.jaspi.WebJASPICallbackHandler;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.callback.PasswordValidationCallback;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.Map;

/**
 * <p>
 * Base class for JBoss Web JSR-196 {@code ServerAuthModule}s.
 * </p>
 *
 * @author <a href="mailto:Anil.Saldhana@redhat.com">Anil Saldhana</a>
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public abstract class WebServerAuthModule implements ServerAuthModule {

    protected CallbackHandler callbackHandler;

    protected Map options;

    @Override
    public Class[] getSupportedMessageTypes() {
        return new Class[]{Request.class, Response.class,
                HttpServletRequest.class, HttpServletResponse.class};
    }

    @Override
    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy,
                           CallbackHandler handler, Map options) throws AuthException {
        this.callbackHandler = handler;
        this.options = options;
    }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
        Request request = (Request) messageInfo.getRequestMessage();
        Principal principal = request.getUserPrincipal();
        if (subject != null)
            subject.getPrincipals().remove(principal);
    }

    @Override
    public abstract AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException;

    @Override
    public abstract AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject,
                                               Subject serviceSubject) throws AuthException;

    /**
     * <p>
     * Register the obtained security attributes with the CallbackHandler.
     * </p>
     *
     * @param userPrincipal the user principal.
     * @param username      a {@code String} representing the username.
     * @param password      a {@code String} representing the password.
     */
    protected void registerWithCallbackHandler(Principal userPrincipal, String username,
                                               String password) {
        if (this.callbackHandler instanceof WebJASPICallbackHandler) {
            WebJASPICallbackHandler cbh = (WebJASPICallbackHandler) callbackHandler;

            PasswordValidationCallback passwordValidationCallback =
                    new PasswordValidationCallback(null, username, password.toCharArray());
            CallerPrincipalCallback callerCallback = new CallerPrincipalCallback(null, userPrincipal);
            try {
                cbh.handle(new Callback[] {passwordValidationCallback, callerCallback});
            } catch (Exception e) {
                throw new RuntimeException("Error handling callbacks: " + e.getLocalizedMessage(), e);
            }
        } else
            throw new RuntimeException(" Unsupported Callback handler " + this.callbackHandler.getClass().
                    getCanonicalName());
    }
}
