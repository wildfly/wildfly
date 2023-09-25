/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

    static final String USER_NAME = "$local";

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
