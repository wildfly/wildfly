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

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A CallbackHandler obtaining the users and their passwords from a properties file.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class PropertiesCallbackHandler implements Service<PropertiesCallbackHandler>, DomainCallbackHandler {

    public static final String SERVICE_SUFFIX = "properties";
    private static final Class[] supportedCallbacks = {AuthorizeCallback.class, RealmCallback.class,
                                                       NameCallback.class, PasswordCallback.class};

    private final String realm;
    private final String path;
    private final InjectedValue<String> relativeTo = new InjectedValue<String>();
    private final Properties users = new Properties();

    public PropertiesCallbackHandler(String realm, ModelNode properties) {
        this.realm = realm;
        path = properties.require(PATH).asString();
    }

    /*
     *  Service Methods
     */

    public void start(StartContext context) throws StartException {
        String relativeTo = this.relativeTo.getOptionalValue();
        String file = relativeTo == null ? path : relativeTo + "/" + path;

        File propertiesFile = new File(file);
        try {
            users.load(propertiesFile.toURI().toURL().openStream());
        } catch (MalformedURLException mue) {
            throw new StartException("Unable to load properties", mue);
        } catch (IOException ioe) {
            throw new StartException("Unable to load properties", ioe);
        }
    }

    public void stop(StopContext context) {
        users.clear();
    }

    public PropertiesCallbackHandler getValue() throws IllegalStateException, IllegalArgumentException {
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
        for (Callback current : callbacks) {

            if (current instanceof AuthorizeCallback) {
                toRespondTo.add(current);
            } else if (current instanceof NameCallback) {
                NameCallback nameCallback = (NameCallback) current;
                userName = nameCallback.getDefaultName();
                userFound = users.containsKey(userName);
            } else if (current instanceof PasswordCallback) {
                toRespondTo.add(current);
            } else if (current instanceof RealmCallback) {
                String realm = ((RealmCallback) current).getDefaultText();
                if (this.realm.equals(realm) == false) {
                    // TODO - Check if this needs a real error or of just an unexpected internal error.
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
            }
        }

    }


}
