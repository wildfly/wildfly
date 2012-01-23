/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.web.security.jaspi;

import org.jboss.as.web.WebLogger;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.callback.GroupPrincipalCallback;
import javax.security.auth.message.callback.PasswordValidationCallback;
import java.io.IOException;

/**
 * <p>
 * This class implements a {@code CallbackHandler} for the JASPI Web Profile.
 * </p>
 *
 * @author <a href="mailto:Anil.Saldhana@redhat.com">Anil Saldhana</a>
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@SuppressWarnings("unused")
public class WebJASPICallbackHandler implements CallbackHandler {

    private CallerPrincipalCallback callerPrincipalCallback;

    private PasswordValidationCallback passwordValidationCallback;

    private GroupPrincipalCallback groupPrincipalCallback;

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {

        if (callbacks.length > 0) {
            for (Callback callback : callbacks) {
                if (callback instanceof CallerPrincipalCallback) {
                    CallerPrincipalCallback callerCallback = (CallerPrincipalCallback) callback;
                    if (callerCallback.getPrincipal()  != null)
                        this.callerPrincipalCallback = new CallerPrincipalCallback(callerCallback.getSubject(),
                                callerCallback.getPrincipal());
                    else
                        this.callerPrincipalCallback = new CallerPrincipalCallback(callerCallback.getSubject(),
                                callerCallback.getName());
                } else if (callback instanceof PasswordValidationCallback) {
                    PasswordValidationCallback passCallback = (PasswordValidationCallback) callback;
                    this.passwordValidationCallback = new PasswordValidationCallback(passCallback.getSubject(),
                            passCallback.getUsername(), passCallback.getPassword());
                } else if (callback instanceof GroupPrincipalCallback) {
                    GroupPrincipalCallback groupCallback = (GroupPrincipalCallback) callback;
                    this.groupPrincipalCallback = new GroupPrincipalCallback(groupCallback.getSubject(),
                            groupCallback.getGroups());
                } else
                    WebLogger.WEB_SECURITY_LOGGER.tracef("Callback %s not supported", callback.getClass().getCanonicalName());
            }
        }
    }

    public CallerPrincipalCallback getCallerPrincipalCallback() {
        return callerPrincipalCallback;
    }

    public PasswordValidationCallback getPasswordValidationCallback() {
        return passwordValidationCallback;
    }

    public GroupPrincipalCallback getGroupPrincipalCallback() {
        return groupPrincipalCallback;
    }
}