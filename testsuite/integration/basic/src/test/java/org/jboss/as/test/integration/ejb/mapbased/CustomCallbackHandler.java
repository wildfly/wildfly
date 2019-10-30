/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.mapbased;

import java.io.IOException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;

/**
 * A custom {@link CallbackHandler} which is used in {@link MapBasedInitialContextEjbClientTestCase} and sets the {@link NameCallback}
 * to user name {@link #USER_NAME}
 *
 * @author Jaikiran Pai
 */
public class CustomCallbackHandler implements CallbackHandler {

    static final String USER_NAME = System.getProperty("elytron") == null ? "foo-bar" : "$local";

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (final Callback current : callbacks) {
            if (current instanceof NameCallback) {
                ((NameCallback) current).setName(USER_NAME);
            } else if (current instanceof RealmCallback) {
                ((RealmCallback) current).setText(((RealmCallback) current).getDefaultText());
            }
        }
    }
}
