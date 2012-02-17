/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.jboss.as.web.deployment.mock;


import java.security.Principal;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;
import org.jboss.logging.Logger;


public class MemoryRealm extends RealmBase {

    private static Logger log = Logger.getLogger(MemoryRealm.class);

    // ----------------------------------------------------- Instance Variables


    /**
     * Descriptive information about this Realm implementation.
     */
    protected final String info =
        "org.apache.catalina.realm.MemoryRealm/1.0";


    /**
     * Descriptive information about this Realm implementation.
     */

    protected static final String name = "MemoryRealm";


    /**
     * The set of valid Principals for this Realm, keyed by user name.
     */
    private Map<String, GenericPrincipal> principals = new HashMap<String, GenericPrincipal>();


    // ------------------------------------------------------------- Properties


    /**
     * Return descriptive information about this Realm implementation and
     * the corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {

        return info;

    }


    // --------------------------------------------------------- Public Methods


    /**
     * Return the Principal associated with the specified username and
     * credentials, if there is one; otherwise return <code>null</code>.
     *
     * @param username Username of the Principal to look up
     * @param credentials Password or other credentials to use in
     *  authenticating this username
     */
    public Principal authenticate(String username, String credentials) {

        GenericPrincipal principal =
            (GenericPrincipal) principals.get(username);

        boolean validated = false;
        if (principal != null && credentials != null) {
            if (hasMessageDigest()) {
                // Hex hashes should be compared case-insensitive
                validated = (digest(credentials)
                             .equalsIgnoreCase(principal.getPassword()));
            } else {
                validated =
                    (digest(credentials).equals(principal.getPassword()));
            }
        }

        if (validated) {
            if (log.isDebugEnabled())
                log.debug(sm.getString("memoryRealm.authenticateSuccess", username));
            return (principal);
        } else {
            if (log.isDebugEnabled())
                log.debug(sm.getString("memoryRealm.authenticateFailure", username));
            return (null);
        }

    }


    // -------------------------------------------------------- Package Methods


    /**
     * Add a new user to the in-memory database.
     *
     * @param username User's username
     * @param password User's password (clear text)
     * @param roles Comma-delimited set of roles associated with this user
     */
    void addUser(String username, String password, String roles) {

        // Accumulate the list of roles for this user
        ArrayList<String> list = new ArrayList<String>();
        roles += ",";
        while (true) {
            int comma = roles.indexOf(',');
            if (comma < 0)
                break;
            String role = roles.substring(0, comma).trim();
            list.add(role);
            roles = roles.substring(comma + 1);
        }

        // Construct and cache the Principal for this user
        GenericPrincipal principal =
            new GenericPrincipal(this, username, password, list);
        principals.put(username, principal);

    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Return a short name for this Realm implementation.
     */
    protected String getName() {

        return (name);

    }


    /**
     * Return the password associated with the given principal's user name.
     */
    protected String getPassword(String username) {

        GenericPrincipal principal =
            (GenericPrincipal) principals.get(username);
        if (principal != null) {
            return (principal.getPassword());
        } else {
            return (null);
        }

    }


    /**
     * Return the Principal associated with the given user name.
     */
    protected Principal getPrincipal(String username) {

        return (Principal) principals.get(username);

    }

    /**
     * Returns the principals for this realm.
     *
     * @return The principals, keyed by user name (a String)
     */
    protected Map<String, GenericPrincipal> getPrincipals() {
        return principals;
    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * Prepare for active use of the public methods of this Component.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents it from being started
     */
    public synchronized void start() throws LifecycleException {

        // Perform normal superclass initialization
        super.start();

        Properties users = new Properties();
        Properties roles = new Properties();
        if (container instanceof Context) {
            ServletContext servletContext = ((Context) container).getServletContext();
            InputStream usersIs = null;
            InputStream rolesIs = null;
            try {
                usersIs = servletContext.getResourceAsStream("/META-INF/users.properties");
                rolesIs = servletContext.getResourceAsStream("/META-INF/roles.properties");
                if (usersIs != null) {
                    users.load(usersIs);
                }
                if (rolesIs != null) {
                    roles.load(rolesIs);
                }
                Enumeration names = users.propertyNames();
                while (names.hasMoreElements()) {
                    String name = (String) names.nextElement();
                    addUser(name, users.getProperty(name), roles.getProperty(name));
                }
            } catch (IOException e) {
                // Ignore
            } finally {
                if (usersIs != null) {
                    try {
                        usersIs.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
                if (rolesIs != null) {
                    try {
                        rolesIs.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
        }
    }


    /**
     * Gracefully shut down active use of the public methods of this Component.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public synchronized void stop() throws LifecycleException {

        // Perform normal superclass finalization
        super.stop();

        // No shutdown activities required

    }


}
