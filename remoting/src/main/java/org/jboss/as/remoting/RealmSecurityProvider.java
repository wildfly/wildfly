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

import static org.jboss.as.domain.management.RealmConfigurationConstants.DIGEST_PLAIN_TEXT;
import static org.jboss.as.domain.management.RealmConfigurationConstants.LOCAL_DEFAULT_USER;
import static org.jboss.as.remoting.RemotingMessages.MESSAGES;
import static org.xnio.Options.SASL_MECHANISMS;
import static org.xnio.Options.SASL_POLICY_NOANONYMOUS;
import static org.xnio.Options.SASL_POLICY_NOPLAINTEXT;
import static org.xnio.Options.SASL_PROPERTIES;
import static org.xnio.Options.SSL_CLIENT_AUTH_MODE;
import static org.xnio.Options.SSL_ENABLED;
import static org.xnio.Options.SSL_STARTTLS;
import static org.xnio.SslClientAuthMode.REQUESTED;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.net.ssl.SSLContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.jboss.as.controller.security.SubjectUserInfo;
import org.jboss.as.controller.security.UniqueIdUserInfo;
import org.jboss.as.domain.management.AuthenticationMechanism;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.domain.management.security.RealmUser;
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

    static final String SASL_OPT_REALM_PROPERTY = "com.sun.security.sasl.digest.realm";
    static final String SASL_OPT_PRE_DIGESTED_PROPERTY = "org.jboss.sasl.digest.pre_digested";
    static final String SASL_OPT_LOCAL_DEFAULT_USER = "jboss.sasl.local-user.default-user";
    static final String SASL_OPT_LOCAL_USER_CHALLENGE_PATH = "jboss.sasl.local-user.challenge-path";

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

        if (realm == null) {
            mechanisms.add(ANONYMOUS);
            builder.set(SASL_POLICY_NOANONYMOUS, false);
            builder.set(SSL_ENABLED, false);
        } else {
            Set<AuthenticationMechanism> authMechs = realm.getSupportedAuthenticationMechanisms();
            if (authMechs.contains(AuthenticationMechanism.LOCAL)) {
                mechanisms.add(JBOSS_LOCAL_USER);
                Map<String, String> mechConfig = realm.getMechanismConfig(AuthenticationMechanism.LOCAL);
                if (mechConfig.containsKey(LOCAL_DEFAULT_USER)) {
                    properties.add(Property.of(SASL_OPT_LOCAL_DEFAULT_USER, mechConfig.get(LOCAL_DEFAULT_USER)));
                }
                if (tokensDir != null) {
                    properties.add(Property.of(SASL_OPT_LOCAL_USER_CHALLENGE_PATH, tokensDir));
                }
            }

            if (authMechs.contains(AuthenticationMechanism.DIGEST)) {
                mechanisms.add(DIGEST_MD5);
                properties.add(Property.of(SASL_OPT_REALM_PROPERTY, realm.getName()));
                Map<String, String> mechConfig = realm.getMechanismConfig(AuthenticationMechanism.DIGEST);
                boolean plainTextDigest = true;
                if (mechConfig.containsKey(DIGEST_PLAIN_TEXT)) {
                    plainTextDigest = Boolean.parseBoolean(mechConfig.get(DIGEST_PLAIN_TEXT));
                }

                if (plainTextDigest == false) {
                    properties.add(Property.of(SASL_OPT_PRE_DIGESTED_PROPERTY, Boolean.TRUE.toString()));
                }
            }

            if (authMechs.contains(AuthenticationMechanism.PLAIN)) {
                mechanisms.add(PLAIN);
                builder.set(SASL_POLICY_NOPLAINTEXT, false);
            }

            if (realm.getSSLContext() == null) {
                builder.set(SSL_ENABLED, false);
            } else {
                if (authMechs.contains(AuthenticationMechanism.CLIENT_CERT)) {
                    builder.set(SSL_ENABLED, true);
                    builder.set(SSL_STARTTLS, true);
                    mechanisms.add(0, EXTERNAL);
                    // TODO - If no other mechanisms are available we can use REQUIRED.
                    builder.set(SSL_CLIENT_AUTH_MODE, REQUESTED);
                } else {
                    builder.set(SSL_ENABLED, true);
                    builder.set(SSL_STARTTLS, true);
                }
            }

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
            return new RealmCallbackHandler(new org.jboss.as.domain.management.AuthorizingCallbackHandler() {

                public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                    for (Callback current : callbacks) {
                        throw MESSAGES.anonymousMechanismNotExpected(current);
                    }
                }

                public SubjectUserInfo createSubjectUserInfo(Collection<Principal> remotingPrincipals) throws IOException {
                    final Subject subject = new Subject();
                    Collection<Principal> allPrincipals = subject.getPrincipals();
                    for (Principal userPrincipal : remotingPrincipals) {
                        allPrincipals.add(userPrincipal);
                        if (userPrincipal instanceof UserPrincipal) {
                            allPrincipals.add(new RealmUser(userPrincipal.getName()));
                        }
                    }

                    final String userName = subject.getPrincipals(RealmUser.class).iterator().next().getName();

                    return new SubjectUserInfo() {

                        public String getUserName() {
                            return userName;
                        }

                        public Subject getSubject() {
                            return subject;
                        }

                        public Collection<Principal> getPrincipals() {
                            return subject.getPrincipals();
                        }
                    };
                }
            });
        }

        if (JBOSS_LOCAL_USER.equals(mechanismName)) {
            // We now only enable this mechanism is configured in the realm so the realm can not be null.
            return new RealmCallbackHandler(realm.getAuthorizingCallbackHandler(AuthenticationMechanism.LOCAL));
        }

        // In this calls only the AuthorizeCallback is needed, we are not making use if an authorization ID just yet
        // so don't need to be linked back to the realms.
        if (EXTERNAL.equals(mechanismName)) {
            return new RealmCallbackHandler(realm.getAuthorizingCallbackHandler(AuthenticationMechanism.CLIENT_CERT));
        }

        final RealmCallbackHandler realmCallbackHandler; // Referenced later by an inner-class so needs to be final.

        if (DIGEST_MD5.equals(mechanismName)) {
            realmCallbackHandler = new RealmCallbackHandler(realm.getAuthorizingCallbackHandler(AuthenticationMechanism.DIGEST));
        } else if (PLAIN.equals(mechanismName)) {
            realmCallbackHandler = new RealmCallbackHandler(realm.getAuthorizingCallbackHandler(AuthenticationMechanism.PLAIN));
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

    private class RealmCallbackHandler implements AuthorizingCallbackHandler {

        private final org.jboss.as.domain.management.AuthorizingCallbackHandler innerHandler;

        RealmCallbackHandler(org.jboss.as.domain.management.AuthorizingCallbackHandler innerHandler) {
            this.innerHandler = innerHandler;
        }

        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            innerHandler.handle(callbacks);
        }

        public UserInfo createUserInfo(Collection<Principal> remotingPrincipals) throws IOException {
            Collection<Principal> converted = new ArrayList<Principal>(remotingPrincipals.size());
            for (Principal current : remotingPrincipals) {
                // Just convert the Remoting UserPrincipal to a RealmUser.
                // The remaining principals will be added to the Subject later.
                if (current instanceof UserPrincipal) {
                    if (realm != null) {
                        converted.add(new RealmUser(realm.getName(), current.getName()));
                    } else {
                        converted.add(new RealmUser(current.getName()));
                    }
                }
            }
            SubjectUserInfo sui = innerHandler.createSubjectUserInfo(converted);
            Subject subject = sui.getSubject();
            subject.getPrincipals().addAll(remotingPrincipals);

            return new RealmSubjectUserInfo(sui);
        }
    }

    private static class RealmSubjectUserInfo implements SubjectUserInfo, UserInfo, UniqueIdUserInfo {

        private final SubjectUserInfo subjectUserInfo;
        private final String id;

        private RealmSubjectUserInfo(SubjectUserInfo subjectUserInfo) {
            this.subjectUserInfo = subjectUserInfo;
            id = UUID.randomUUID().toString();
        }

        public String getUserName() {
            return subjectUserInfo.getUserName();
        }

        public Collection<Principal> getPrincipals() {
            return subjectUserInfo.getPrincipals();
        }

        public Subject getSubject() {
            return subjectUserInfo.getSubject();
        }

        public String getId() {
            return id;
        }

    }

}
