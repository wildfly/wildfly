package org.jboss.as.appclient.service;

import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.Set;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;

import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.jboss.security.identity.Identity;
import org.jboss.security.identity.extensions.CredentialIdentity;

import static java.security.AccessController.doPrivileged;

/**
 * The default callback handler used by the
 *
 * @author Stuart Douglas
 */
public class DefaultApplicationClientCallbackHandler implements CallbackHandler {

    public static final String ANONYMOUS = "anonymous";

    @Override
    public void handle(final Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        final SecurityContext context = doPrivileged(securityContext());

        for (final Callback current : callbacks) {
            if (current instanceof NameCallback) {
                final NameCallback ncb = (NameCallback) current;
                if (context != null) {
                    final Set<Identity> identities = context.getSubjectInfo().getIdentities();
                    if (identities.isEmpty()) {
                        ncb.setName(ANONYMOUS);
                    } else {
                        final Identity identity = identities.iterator().next();
                        ncb.setName(identity.getName());
                    }
                } else {
                    ncb.setName(ANONYMOUS);
                }
            } else if (current instanceof PasswordCallback) {
                if (context != null) {
                    final PasswordCallback pcb = (PasswordCallback) current;
                    final Set<Identity> identities = context.getSubjectInfo().getIdentities();
                    if (identities.isEmpty()) {
                        throw new UnsupportedCallbackException(current);
                    } else {
                        final Identity identity = identities.iterator().next();
                        if (identity instanceof CredentialIdentity) {
                            pcb.setPassword((char[]) ((CredentialIdentity) identity).getCredential());
                        } else {
                            throw new UnsupportedCallbackException(current);
                        }
                    }
                }
            } else if (current instanceof RealmCallback) {
                final RealmCallback realmCallback = (RealmCallback) current;
                if (realmCallback.getText() == null) {
                    realmCallback.setText(realmCallback.getDefaultText());
                }
            }
        }
    }


    private static PrivilegedAction<SecurityContext> securityContext() {
        return new PrivilegedAction<SecurityContext>() {
            public SecurityContext run() {
                return SecurityContextAssociation.getSecurityContext();
            }
        };
    }
}
