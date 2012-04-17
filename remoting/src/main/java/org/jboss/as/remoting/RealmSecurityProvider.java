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
package org.jboss.as.remoting;

import static org.jboss.as.remoting.RemotingMessages.MESSAGES;
import static org.xnio.Options.SASL_MECHANISMS;
import static org.xnio.Options.SASL_POLICY_NOANONYMOUS;
import static org.xnio.Options.SASL_POLICY_NOPLAINTEXT;
import static org.xnio.Options.SASL_PROPERTIES;
import static org.xnio.Options.SSL_ENABLED;
import static org.xnio.Options.SSL_STARTTLS;
import static org.xnio.Options.SSL_CLIENT_AUTH_MODE;
import static org.xnio.SslClientAuthMode.REQUESTED;

import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.net.ssl.SSLContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;

import org.jboss.as.controller.security.SubjectUserInfo;
import org.jboss.as.controller.security.UniqueIdUserInfo;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.domain.management.security.DomainCallbackHandler;
import org.jboss.as.domain.management.security.LocalCallbackHandler;
import org.jboss.as.domain.management.security.RealmUser;
import org.jboss.as.domain.management.security.SubjectCallback;
import org.jboss.as.domain.management.security.SubjectSupplemental;
import org.jboss.remoting3.Remoting;
import org.jboss.remoting3.security.AuthorizingCallbackHandler;
import org.jboss.remoting3.security.ServerAuthenticationProvider;
import org.jboss.remoting3.security.SimpleUserInfo;
import org.jboss.remoting3.security.UserInfo;
import org.jboss.remoting3.security.UserPrincipal;
import org.jboss.sasl.callback.DigestHashCallback;
import org.jboss.sasl.callback.VerifyPasswordCallback;
import org.xnio.OptionMap;
import org.xnio.OptionMap.Builder;
import org.xnio.Property;
import org.xnio.Sequence;
import org.xnio.Xnio;
import org.xnio.ssl.JsseXnioSsl;
import org.xnio.ssl.XnioSsl;

/**
 * A Remoting ServerAuthenticationProvider that wraps a management domain security realm.
 * <p/>
 * In addition to making the CallbackHandler available for the requested mechanism this class will also generate the initial
 * OptionsMap based on the capabilities of the security realm.
 * <p/>
 * Initially this only verified that DIGEST-MD5 can be supported.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class RealmSecurityProvider implements RemotingSecurityProvider {

    static final String REALM_PROPERTY = "com.sun.security.sasl.digest.realm";
    static final String PRE_DIGESTED_PROPERTY = "org.jboss.sasl.digest.pre_digested";
    static final String LOCAL_DEFAULT_USER = "jboss.sasl.local-user.default-user";
    static final String LOCAL_USER_CHALLENGE_PATH = "jboss.sasl.local-user.challenge-path";

    static final String ANONYMOUS = "ANONYMOUS";
    static final String DIGEST_MD5 = "DIGEST-MD5";
    static final String EXTERNAL = "EXTERNAL";
    static final String JBOSS_LOCAL_USER = "JBOSS-LOCAL-USER";
    static final String PLAIN = "PLAIN";

    private final SecurityRealm realm;
    private final CallbackHandler serverCallbackHandler;
    private final String tokensDir;

    RealmSecurityProvider(final SecurityRealm realm, final CallbackHandler serverCallbackHandler, final String tokensDir) {
        this.realm = realm;
        this.serverCallbackHandler = serverCallbackHandler;
        this.tokensDir = tokensDir;
    }

    /*
     * RemotingSecurityProvider methods.
     */

    @Override
    public OptionMap getOptionMap() {
        List<String> mechanisms = new LinkedList<String>();
        Set<Property> properties = new HashSet<Property>();
        Builder builder = OptionMap.builder();

        LocalCallbackHandler localHandler = realm != null ? realm.getLocalCallbackHandler() : null;
        if (localHandler != null) {
            mechanisms.add(JBOSS_LOCAL_USER);

            String defaultUser = localHandler.getDefaultUser();
            if (defaultUser != null) {
                properties.add(Property.of(LOCAL_DEFAULT_USER, defaultUser));
            }
            if (tokensDir != null) {
                properties.add(Property.of(LOCAL_USER_CHALLENGE_PATH, tokensDir));
            }
        }

        builder.set(SASL_POLICY_NOPLAINTEXT, false);
        if (digestMd5Supported()) {
            mechanisms.add(DIGEST_MD5);
            properties.add(Property.of(REALM_PROPERTY, realm.getName()));
            if (contains(DigestHashCallback.class, realm.getCallbackHandler().getSupportedCallbacks())) {
                properties.add(Property.of(PRE_DIGESTED_PROPERTY, Boolean.TRUE.toString()));
            }
        } else if (plainSupported()) {
            mechanisms.add(PLAIN);
        } else if (realm == null) {
            mechanisms.add(ANONYMOUS);
            builder.set(SASL_POLICY_NOANONYMOUS, false);
        }

        SslMode sslMode = getSslMode();
        switch (sslMode) {
            case OFF:
                builder.set(SSL_ENABLED, false);
                break;
            case TRANSPORT_ONLY:
                builder.set(SSL_ENABLED, true);
                builder.set(SSL_STARTTLS, true);
                break;
            case CLIENT_AUTH_REQUESTED:
                builder.set(SSL_ENABLED, true);
                builder.set(SSL_STARTTLS, true);
                mechanisms.add(0, EXTERNAL);
                builder.set(SSL_CLIENT_AUTH_MODE, REQUESTED);
                break;
        // We do not currently support the SSL_CLIENT_AUTH_MODE of REQUIRED as there is always
        // the possibility that the local mechanism will still be needed.
        }

        if (mechanisms.size() == 0) {
            throw MESSAGES.noSupportingMechanismsForRealm();
        }

        builder.set(SASL_MECHANISMS, Sequence.of(mechanisms));
        builder.set(SASL_PROPERTIES, Sequence.of(properties));

        return builder.getMap();
    }

    @Override
    public ServerAuthenticationProvider getServerAuthenticationProvider() {
        return new ServerAuthenticationProvider() {

            @Override
            public AuthorizingCallbackHandler getCallbackHandler(String mechanismName) {
                final CallbackHandler cbh = RealmSecurityProvider.this.getCallbackHandler(mechanismName);
                if (cbh instanceof AuthorizingCallbackHandler) {
                    return (AuthorizingCallbackHandler) cbh;
                } else {
                    return new AuthorizingCallbackHandler() {

                        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                            cbh.handle(callbacks);
                        }

                        public UserInfo createUserInfo(final Collection<Principal> remotingPrincipals) {
                            return new SimpleUserInfo(remotingPrincipals);
                        }
                    };
                }
            }
        };
    }

    @Override
    public XnioSsl getXnioSsl() {
        final SSLContext sslContext;
        if (realm == null || (sslContext = realm.getSSLContext()) == null) {
            return null;
        }

        return new JsseXnioSsl(Xnio.getInstance(Remoting.class.getClassLoader()), OptionMap.EMPTY, sslContext);
    }

    /*
     * Internal methods.
     */

    public CallbackHandler getCallbackHandler(String mechanismName) {
        // TODO - Once authorization is in place we may be able to relax the realm check to
        // allow anonymous along side fully authenticated connections.

        // If the mechanism is ANONYMOUS and we don't have a realm we return quickly.
        if (ANONYMOUS.equals(mechanismName) && realm == null) {
            return new RealmCallbackHandler(new CallbackHandler() {

                public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                    for (Callback current : callbacks) {
                        throw MESSAGES.anonymousMechanismNotExpected(current);
                    }
                }
            }, null);
        }

        // For now for the JBOSS_LOCAL_USER we are only supporting the $local user and not allowing for
        // an alternative authorizationID.
        if (JBOSS_LOCAL_USER.equals(mechanismName)) {
            // We now only enable this mechanism is configured in the realm so the realm can not be null.
            return new RealmCallbackHandler(realm.getLocalCallbackHandler(), realm.getSubjectSupplemental());
        }

        // In this calls only the AuthorizeCallback is needed, we are not making use if an authorization ID just yet
        // so don't need to be linked back to the realms.
        if (EXTERNAL.equals(mechanismName)) {
            return new RealmCallbackHandler(new CallbackHandler() {

                @Override
                public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                    for (Callback current : callbacks) {
                        if (current instanceof AuthorizeCallback) {
                            AuthorizeCallback acb = (AuthorizeCallback) current;
                            acb.setAuthorized(acb.getAuthenticationID().equals(acb.getAuthorizationID()));
                        } else {
                            throw MESSAGES.unsupportedCallback(current);
                        }
                    }

                }
            }, realm.getSubjectSupplemental());
        }

        final RealmCallbackHandler realmCallbackHandler; // Referenced later by an inner-class so needs to be final.

        // TODO - Although we recommend Digest auth in the default config if the subsytem is overriden then we may get PLAIN so
        //        double check it still works.

        // We must have a match in this block or throw an IllegalStateException.
        if (DIGEST_MD5.equals(mechanismName) && digestMd5Supported() || PLAIN.equals(mechanismName) && plainSupported()) {
            realmCallbackHandler = new RealmCallbackHandler(realm.getCallbackHandler(), realm.getSubjectSupplemental());
        } else {
            return null;
        }

        // If there is not serverCallbackHandler then we don't need to wrap it so we can just return the realm
        // name fix handler which is already wrapping the real handler.
        if (serverCallbackHandler == null) {
            return realmCallbackHandler;
        }

        return new AuthorizingCallbackHandler() {

            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                serverCallbackHandler.handle(callbacks);
                if (handled(callbacks) == false) {
                    realmCallbackHandler.handle(callbacks);
                }
            }

            /*
             * Check if the PasswordCallback had already been handled.
             */
            private boolean handled(Callback[] callbacks) {
                // For the moment handled will only return true if the user was successfully authenticated,
                // may later add a Callback to obtain the server info to verify is a server was identified with
                // the specified username.

                for (Callback current : callbacks) {
                    if (current instanceof PasswordCallback) {
                        PasswordCallback pcb = (PasswordCallback) current;
                        char[] password = pcb.getPassword();
                        return (password != null && password.length > 0);
                    } else if (current instanceof VerifyPasswordCallback) {
                        return ((VerifyPasswordCallback) current).isVerified();
                    } else if (current instanceof DigestHashCallback) {
                        return ((DigestHashCallback) current).getHash() != null;
                    }
                }
                return false;
            }

            public UserInfo createUserInfo(Collection<Principal> remotingPrincipals) throws IOException {
                return realmCallbackHandler.createUserInfo(remotingPrincipals);
            }

        };
    }

    private SslMode getSslMode() {
        if (realm == null || realm.getSSLContext() == null) {
            // No SSL Context to no way can we enable SSL.
            return SslMode.OFF;
        }

        if (realm.hasTrustStore()) {
            return SslMode.CLIENT_AUTH_REQUESTED;
        }

        return SslMode.TRANSPORT_ONLY;
    }

    private enum SslMode {
        OFF, TRANSPORT_ONLY, CLIENT_AUTH_REQUESTED
    }

    private boolean digestMd5Supported() {
        if (realm == null) {
            return false;
        }

        Class<Callback>[] callbacks = realm.getCallbackHandler().getSupportedCallbacks();
        if (contains(NameCallback.class, callbacks) == false) {
            return false;
        }
        if (contains(RealmCallback.class, callbacks) == false) {
            return false;
        }
        if (contains(PasswordCallback.class, callbacks) == false && contains(DigestHashCallback.class, callbacks) == false) {
            return false;
        }
        if (contains(AuthorizeCallback.class, callbacks) == false) {
            return false;
        }

        return true;
    }

    private boolean plainSupported() {
        if (realm == null) {
            return false;
        }

        Class<Callback>[] callbacks = realm.getCallbackHandler().getSupportedCallbacks();
        if (contains(NameCallback.class, callbacks) == false) {
            return false;
        }
        if (contains(VerifyPasswordCallback.class, callbacks) == false) {
            return false;
        }
        if (contains(AuthorizeCallback.class, callbacks) == false) {
            return false;
        }

        return true;
    }

    private static boolean contains(Class clazz, Class<Callback>[] classes) {
        for (Class<Callback> current : classes) {
            if (current.equals(clazz)) {
                return true;
            }
        }
        return false;
    }

    private class RealmCallbackHandler implements AuthorizingCallbackHandler {

        private final CallbackHandler callbackHandler;
        private final SubjectSupplemental subjectSupplemental;
        private final boolean subjectCallbackSupported;

        /** The Subject returned from the authentication process. */
        private Subject subject = null;

        private RealmCallbackHandler(CallbackHandler callbackHandler, SubjectSupplemental subjectSupplemental) {
            this.callbackHandler = callbackHandler;
            this.subjectSupplemental = subjectSupplemental;
            subjectCallbackSupported = false;
        }

        private RealmCallbackHandler(DomainCallbackHandler callbackHandler, SubjectSupplemental subjectSupplemental) {
            this.callbackHandler = callbackHandler;
            this.subjectSupplemental = subjectSupplemental;
            subjectCallbackSupported = contains(SubjectCallback.class, callbackHandler.getSupportedCallbacks());
        }

        @Override
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            if (subjectCallbackSupported && callbacks.length != 1 && callbacks[0] instanceof AuthorizeCallback == false) {
                Callback[] newCallbacks = new Callback[callbacks.length + 1];
                System.arraycopy(callbacks, 0, newCallbacks, 0, callbacks.length);
                SubjectCallback subjectCallBack = new SubjectCallback();
                newCallbacks[newCallbacks.length - 1] = subjectCallBack;
                callbackHandler.handle(newCallbacks);
                subject = subjectCallBack.getSubject();
            } else {
                callbackHandler.handle(callbacks);
            }
        }

        @Override
        public UserInfo createUserInfo(Collection<Principal> remotingPrincipals) throws IOException {
            Subject subject = this.subject == null ? new Subject() : this.subject;
            subject.getPrincipals().addAll(remotingPrincipals);
            Set<UserPrincipal> remotingUsers = subject.getPrincipals(UserPrincipal.class);
            Set<RealmUser> realmUsers = new HashSet<RealmUser>(remotingUsers.size());
            for (UserPrincipal current : remotingUsers) {
                if (realm != null) {
                    realmUsers.add(new RealmUser(realm.getName(), current.getName()));
                } else {
                    realmUsers.add(new RealmUser(current.getName()));
                }
            }
            subject.getPrincipals().addAll(realmUsers);

            if (subjectSupplemental != null) {
                subjectSupplemental.supplementSubject(subject);
            }

            return new RealmSubjectUserInfo(subject);
        }
    }

    private static class RealmSubjectUserInfo implements SubjectUserInfo, UserInfo, UniqueIdUserInfo {

        private final String userName;
        private final Subject subject;
        private final String id;

        private RealmSubjectUserInfo(Subject subject) {
            this.subject = subject;
            Set<RealmUser> userPrinc = subject.getPrincipals(RealmUser.class);
            userName = userPrinc.isEmpty() ? null : userPrinc.iterator().next().getName();
            id = UUID.randomUUID().toString();
        }

        public String getUserName() {
            return userName;
        }

        public Collection<Principal> getPrincipals() {
            return subject.getPrincipals();
        }

        public Subject getSubject() {
            return subject;
        }

        public String getId() {
            return id;
        }

    }

}
