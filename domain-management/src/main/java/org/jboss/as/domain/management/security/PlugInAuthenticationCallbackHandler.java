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

package org.jboss.as.domain.management.security;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;
import static org.jboss.as.domain.management.ModelDescriptionConstants.MECHANISM;
import static org.jboss.as.domain.management.RealmConfigurationConstants.DIGEST_PLAIN_TEXT;
import static org.jboss.as.domain.management.RealmConfigurationConstants.VERIFY_PASSWORD_CALLBACK_SUPPORTED;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;

import org.jboss.as.domain.management.AuthenticationMechanism;
import org.jboss.as.domain.management.plugin.AuthenticationPlugIn;
import org.jboss.as.domain.management.plugin.Credential;
import org.jboss.as.domain.management.plugin.DigestCredential;
import org.jboss.as.domain.management.plugin.Identity;
import org.jboss.as.domain.management.plugin.PasswordCredential;
import org.jboss.as.domain.management.plugin.PlugInConfigurationSupport;
import org.jboss.as.domain.management.plugin.ValidatePasswordCredential;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.sasl.callback.DigestHashCallback;
import org.jboss.sasl.callback.VerifyPasswordCallback;
import org.jboss.sasl.util.UsernamePasswordHashUtil;

/**
 * CallbackHandlerService to integrate the plug-ins.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class PlugInAuthenticationCallbackHandler extends AbstractPlugInService implements Service<CallbackHandlerService>,
        CallbackHandlerService {

    public static final String SERVICE_SUFFIX = "plug-in-authentication";

    private static UsernamePasswordHashUtil hashUtil = null;

    private final ModelNode model;
    private final String realmName;
    private AuthenticationMechanism mechanism;

    PlugInAuthenticationCallbackHandler(final String realmName,
            final ModelNode model) {
        super(model);
        this.realmName = realmName;
        this.model = model;
    }

    /*
     * Service Methods
     */

    @Override
    public void start(final StartContext context) throws StartException {
        super.start(context);
        if (model.hasDefined(MECHANISM)) {
            mechanism = AuthenticationMechanism.valueOf(model.require(MECHANISM).asString());
        } else {
            mechanism = AuthenticationMechanism.DIGEST;
        }
    }

    public void stop(final StopContext context) {
        mechanism = null;
    }

    public CallbackHandlerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    private static UsernamePasswordHashUtil getHashUtil() {
        if (hashUtil == null) {
            try {
                hashUtil = new UsernamePasswordHashUtil();
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }
        return hashUtil;
    }

    /*
     * CallbackHandlerService Methods
     */

    public AuthenticationMechanism getPreferredMechanism() {
        return mechanism;
    }

    public Set<AuthenticationMechanism> getSupplementaryMechanisms() {
        return Collections.emptySet();
    }

    public Map<String, String> getConfigurationOptions() {
        if (mechanism == AuthenticationMechanism.DIGEST) {
            // If the plug-in returns a plain text password we can hash it.
            return Collections.singletonMap(DIGEST_PLAIN_TEXT, Boolean.FALSE.toString());
        } else {
            return Collections.singletonMap(VERIFY_PASSWORD_CALLBACK_SUPPORTED, Boolean.TRUE.toString());
        }
    }

    public boolean isReady() {
        return true;
    }

    public CallbackHandler getCallbackHandler(final Map<String, Object> sharedState) {
        final String name = getPlugInName();
        final AuthenticationPlugIn<Credential> ap = getPlugInLoader().loadAuthenticationPlugIn(name);
        if (ap instanceof PlugInConfigurationSupport) {
            PlugInConfigurationSupport pcf = (PlugInConfigurationSupport) ap;
            try {
                pcf.init(getConfiguration(), sharedState);
            } catch (IOException e) {
                throw MESSAGES.unableToInitialisePlugIn(name, e.getMessage());
            }
        }

        return new CallbackHandler() {

            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                List<Callback> toRespondTo = new LinkedList<Callback>();

                String userName = null;
                Credential credential = null;

                // A single pass may be sufficient but by using a two pass approach the Callbackhandler will not
                // fail if an unexpected order is encountered.

                // First Pass - is to double check no unsupported callbacks and to retrieve
                // information from the callbacks passing in information.
                for (Callback current : callbacks) {
                    if (current instanceof AuthorizeCallback) {
                        toRespondTo.add(current);
                    } else if (current instanceof NameCallback) {
                        NameCallback nameCallback = (NameCallback) current;
                        userName = nameCallback.getDefaultName();
                        Identity identity = ap.loadIdentity(userName, realmName);
                        if (identity != null) {
                            credential = identity.getCredential();
                        }
                    } else if (current instanceof PasswordCallback) {
                        toRespondTo.add(current);
                    } else if (current instanceof DigestHashCallback) {
                        toRespondTo.add(current);
                    } else if (current instanceof VerifyPasswordCallback) {
                        toRespondTo.add(current);
                    } else if (current instanceof RealmCallback) {
                        String realm = ((RealmCallback) current).getDefaultText();
                        if (realmName.equals(realm) == false) {
                            throw MESSAGES.invalidRealm(realm, realmName);
                        }
                    } else {
                        throw new UnsupportedCallbackException(current);
                    }
                }

                // Second Pass - Now iterate the Callback(s) requiring a response.
                for (Callback current : toRespondTo) {
                    if (current instanceof AuthorizeCallback) {
                        AuthorizeCallback authorizeCallback = (AuthorizeCallback) current;
                        // Don't support impersonating another identity
                        authorizeCallback.setAuthorized(authorizeCallback.getAuthenticationID().equals(
                                authorizeCallback.getAuthorizationID()));
                    } else if (current instanceof PasswordCallback) {
                        if (credential == null) {
                            throw new UserNotFoundException(userName);
                        }

                        if (credential instanceof PasswordCredential) {
                            ((PasswordCallback) current).setPassword(((PasswordCredential) credential).getPassword());
                        } else {
                            throw new UnsupportedCallbackException(current);
                        }
                    } else if (current instanceof DigestHashCallback) {
                        if (credential == null) {
                            throw new UserNotFoundException(userName);
                        }

                        if (credential instanceof DigestCredential) {
                            ((DigestHashCallback) current).setHexHash(((DigestCredential) credential).getHash());
                        } else if (credential instanceof PasswordCredential) {
                            UsernamePasswordHashUtil hashUtil = getHashUtil();
                            String hash;
                            synchronized (hashUtil) {
                                hash = hashUtil.generateHashedHexURP(userName, realmName,
                                        ((PasswordCredential) credential).getPassword());
                            }
                            ((DigestHashCallback) current).setHexHash(hash);
                        } else {
                            throw new UnsupportedCallbackException(current);
                        }
                    } else if (current instanceof VerifyPasswordCallback) {
                        if (credential == null) {
                            throw new UserNotFoundException(userName);
                        }

                        VerifyPasswordCallback vpc = (VerifyPasswordCallback) current;

                        if (credential instanceof PasswordCredential) {
                            vpc.setVerified(Arrays.equals(((PasswordCredential) credential).getPassword(), vpc.getPassword()
                                    .toCharArray()));
                        } else if (credential instanceof DigestCredential) {
                            UsernamePasswordHashUtil hashUtil = getHashUtil();
                            String hash;
                            synchronized (hashUtil) {
                                hash = hashUtil.generateHashedHexURP(userName, realmName, vpc.getPassword().toCharArray());
                            }
                            String expected = ((DigestCredential) credential).getHash();
                            vpc.setVerified(expected.equals(hash));
                        } else if (credential instanceof ValidatePasswordCredential) {
                            vpc.setVerified(((ValidatePasswordCredential) credential).validatePassword(vpc.getPassword()
                                    .toCharArray()));
                        }

                    }
                }
            }
        };

    }

}
