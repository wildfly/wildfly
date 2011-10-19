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

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.jboss.logging.Logger;

import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.sasl.callback.DigestHashCallback;

/**
 * A CallbackHandler obtaining the users and their passwords from a properties file.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class PropertiesCallbackHandler implements Service<DomainCallbackHandler>, DomainCallbackHandler {

    private static final Logger log = Logger.getLogger("org.jboss.as.domain-management");
    public static final String SERVICE_SUFFIX = "properties";

    // Technically this CallbackHandler could also support the VerifyCallback callback, however at the moment
    // this is only likely to be used with the Digest mechanism so no need to add that support.
    private static final Class[] PLAIN_CALLBACKS = {AuthorizeCallback.class, RealmCallback.class,
            NameCallback.class, PasswordCallback.class};
    private static final Class[] DIGEST_CALLBACKS = {AuthorizeCallback.class, RealmCallback.class,
            NameCallback.class, DigestHashCallback.class};

    private final Class[] supportedCallbacks;

    private final String realm;
    private final String path;
    private final boolean plainText;
    private final InjectedValue<String> relativeTo = new InjectedValue<String>();

    private File propertiesFile;
    private volatile long fileUpdated = -1;
    private volatile Properties userProperties = null;

    public PropertiesCallbackHandler(String realm, ModelNode properties) {
        this.realm = realm;
        path = properties.require(PATH).asString();
        if (properties.hasDefined(PLAIN_TEXT)) {
            plainText = properties.require(PLAIN_TEXT).asBoolean();
        } else {
            plainText = false;
        }
        supportedCallbacks = plainText ? PLAIN_CALLBACKS : DIGEST_CALLBACKS;
    }

    /*
     *  Service Methods
     */

    public void start(StartContext context) throws StartException {
        String relativeTo = this.relativeTo.getOptionalValue();
        String file = relativeTo == null ? path : relativeTo + "/" + path;

        propertiesFile = new File(file);
        try {
            getUsersProperties();
        } catch (IOException ioe) {
            throw new StartException("Unable to load properties", ioe);
        }
    }

    private Properties getUsersProperties() throws IOException {
        /*
         *  This method does attempt to minimise the effect of race conditions, however this is not overly critical
         *  as if you have users attempting to authenticate at the exact point their details are added to the file there
         *  is also a change of a race.
         */

        boolean loadRequired = userProperties == null || fileUpdated != propertiesFile.lastModified();

        if (loadRequired) {
            synchronized (this) {
                // Cache the value as there is still a chance of further modification.
                long fileLastModified = propertiesFile.lastModified();
                boolean loadReallyRequired = userProperties == null || fileUpdated != fileLastModified;
                if (loadReallyRequired) {
                    log.debugf("Reloading properties file '%s%", propertiesFile.getAbsolutePath());
                    Properties props = new Properties();
                    InputStream is = new FileInputStream(propertiesFile);
                    try {
                        props.load(is);
                    } finally {
                        is.close();
                    }
                    checkWeakPasswords(props);

                    userProperties = props;
                    // Update this last otherwise the check outside the synchronized block could return true before the file is set.
                    fileUpdated = fileLastModified;
                }
            }
        }

        return userProperties;
    }

    private void checkWeakPasswords(final Properties properties) {
        final String admin = "admin";
        if (properties.contains(admin) && admin.equals(properties.get(admin))) {
            log.warn("Properties file defined with default user and password, this will be easy to guess.");
        }
    }

    public void stop(StopContext context) {
        userProperties.clear();
        userProperties = null;
        propertiesFile = null;
    }

    public DomainCallbackHandler getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /*
     *  Injector Accessors
     */

    public InjectedValue<String> getRelativeToInjector() {
        return relativeTo;
    }

    /*
     *  DomainCallbackHandler Methods
     */

    public Class[] getSupportedCallbacks() {
        return supportedCallbacks;
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        List<Callback> toRespondTo = new LinkedList<Callback>();

        String userName = null;
        boolean userFound = false;

        // A single pass may be sufficient but by using a two pass approach the Callbackhandler will not
        // fail if an unexpected order is encountered.

        // First Pass - is to double check no unsupported callbacks and to retrieve
        // information from the callbacks passing in information.
        Properties users = getUsersProperties();
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
                    throw new IllegalStateException("Invalid Realm '" + realm + "' expected '" + this.realm + "'");
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
                authorizeCallback.setAuthorized(authorizeCallback.getAuthenticationID().equals(authorizeCallback.getAuthorizationID()));
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
