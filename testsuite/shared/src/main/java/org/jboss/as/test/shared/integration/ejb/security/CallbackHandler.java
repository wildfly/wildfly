package org.jboss.as.test.shared.integration.ejb.security;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;

/**
 * @author Stuart Douglas
 */
public class CallbackHandler implements javax.security.auth.callback.CallbackHandler{
    @Override
    public void handle(final Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for(final Callback current : callbacks) {
            if(current instanceof NameCallback) {
                ((NameCallback) current).setName("$local");
            } else if(current instanceof RealmCallback) {
                ((RealmCallback) current).setText(((RealmCallback) current).getDefaultText());
            }
        }

    }
}
