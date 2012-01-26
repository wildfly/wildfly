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

package org.jboss.as.domain.management.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PLAIN_TEXT;
import static org.jboss.as.domain.management.DomainManagementLogger.ROOT_LOGGER;
import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;

import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.sasl.callback.DigestHashCallback;

/**
 * A CallbackHandler obtaining the users and their passwords from a properties file.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class PropertiesCallbackHandler extends PropertiesFileLoader implements Service<DomainCallbackHandler>,
        DomainCallbackHandler {

    public static final String SERVICE_SUFFIX = "properties_authentication";

    // Technically this CallbackHandler could also support the VerifyCallback callback, however at the moment
    // this is only likely to be used with the Digest mechanism so no need to add that support.
    private static final Class[] PLAIN_CALLBACKS = { AuthorizeCallback.class, RealmCallback.class, NameCallback.class,
            PasswordCallback.class };
    private static final Class[] DIGEST_CALLBACKS = { AuthorizeCallback.class, RealmCallback.class, NameCallback.class,
            DigestHashCallback.class };

    private static final String DOLLAR_LOCAL = "$local";

    private final Class[] supportedCallbacks;

    private final String realm;
    private final boolean plainText;

    public PropertiesCallbackHandler(String realm, ModelNode properties) {
        super(properties.require(PATH).asString());
        this.realm = realm;
        if (properties.hasDefined(PLAIN_TEXT)) {
            plainText = properties.require(PLAIN_TEXT).asBoolean();
        } else {
            plainText = false;
        }
        supportedCallbacks = plainText ? PLAIN_CALLBACKS : DIGEST_CALLBACKS;
    }

    /*
     * Service Methods
     */

    public void start(StartContext context) throws StartException {
        super.start(context);
    }

    @Override
    protected void verifyProperties(Properties properties) throws IOException {
        final String admin = "admin";
        if (properties.contains(admin) && admin.equals(properties.get(admin))) {
            ROOT_LOGGER.userAndPasswordWarning();
        }
    }

    public void stop(StopContext context) {
        super.stop(context);
    }

    public DomainCallbackHandler getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /*
     * DomainCallbackHandler Methods
     */

    public Class[] getSupportedCallbacks() {
        return supportedCallbacks;
    }

    @Override
    public boolean isReady() {
        Properties users;
        try {
            users = getProperties();
        } catch (IOException e) {
            return false;
        }
        return (users.size() > 0);
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        List<Callback> toRespondTo = new LinkedList<Callback>();

        String userName = null;
        boolean userFound = false;

        Properties users = getProperties();

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
                userFound = users.containsKey(userName);
            } else if (current instanceof PasswordCallback && plainText) {
                toRespondTo.add(current);
            } else if (current instanceof DigestHashCallback && plainText == false) {
                toRespondTo.add(current);
            } else if (current instanceof RealmCallback) {
                String realm = ((RealmCallback) current).getDefaultText();
                if (this.realm.equals(realm) == false) {
                    throw MESSAGES.invalidRealm(realm, this.realm);
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
                if (userFound == false) {
                    throw new UserNotFoundException(userName);
                }
                String password = users.get(userName).toString();
                ((PasswordCallback) current).setPassword(password.toCharArray());
            } else if (current instanceof DigestHashCallback) {
                if (userFound == false) {
                    throw new UserNotFoundException(userName);
                }
                String hash = users.get(userName).toString();
                ((DigestHashCallback) current).setHexHash(hash);
            }
        }

    }

}
