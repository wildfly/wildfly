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
