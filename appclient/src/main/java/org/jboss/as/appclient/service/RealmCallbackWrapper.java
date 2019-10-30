/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.appclient.service;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;

/**
 * Wrapper for user provided callback handlers that may not know how to handle {@link javax.security.sasl.RealmCallback}
 *
 * @author Stuart Douglas
 */
public class RealmCallbackWrapper implements CallbackHandler {


    private final CallbackHandler callbackHandler;

    public RealmCallbackWrapper(final CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }

    @Override
    public void handle(final Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        callbackHandler.handle(callbacks);
        for(Callback callback : callbacks) {
            if(callback instanceof RealmCallback) {
                final RealmCallback realmCallback = (RealmCallback)callback;
                if(realmCallback.getText() == null) {
                    realmCallback.setText(realmCallback.getDefaultText());
                }
            }
        }
    }
}
