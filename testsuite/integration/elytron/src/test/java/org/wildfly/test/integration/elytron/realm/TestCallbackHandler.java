/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.test.integration.elytron.realm;

import org.wildfly.security.evidence.Evidence;
import org.wildfly.security.evidence.PasswordGuessEvidence;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.security.Principal;

/**
 * A custom {@link CallbackHandler} used in the JAAS security realm tests. It implements the
 * {@code setSecurityInfo} method that has been historically used to populate custom handlers. Also, its {@code handle}
 * implementation will handle any kind of credential by calling {@code toString} and then {@code toCharArray} on the opaque
 * object.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class TestCallbackHandler implements CallbackHandler {

    private Principal principal;
    private Evidence evidence;

    public TestCallbackHandler() {
    }

    /**
     * Sets this handler's state.
     *
     * @param principal the principal being authenticated.
     * @param evidence the evidence being verified.
     */
    public void setSecurityInfo(final Principal principal, final Object evidence) {
        this.principal = principal;
        this.evidence = (Evidence) evidence;
    }

    @Override
    public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
        if (callbacks == null) {
            throw new IllegalArgumentException("The callbacks argument cannot be null");
        }

        for (Callback callback : callbacks) {
            if (callback instanceof NameCallback) {
                NameCallback nameCallback = (NameCallback) callback;
                if (principal != null)
                    nameCallback.setName(this.principal.getName());
            }
            else if (callback instanceof PasswordCallback) {
                PasswordCallback passwordCallback = (PasswordCallback) callback;
                if (this.evidence instanceof PasswordGuessEvidence) {
                    passwordCallback.setPassword(((PasswordGuessEvidence) this.evidence).getGuess());
                }
            }
            else {
                throw new UnsupportedCallbackException(callback, "Unsupported callback");
            }
        }
    }
}
