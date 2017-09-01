/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.multinode.security.api;

import org.jboss.as.test.multinode.security.util.EJBUtil;

/**
 * @author bmaxwell
 *
 */
public class TestConfig {

    public static final String MULTINODE_CLIENT = "multinode-client";
    public static final String MULTINODE_SERVER = "multinode-server";

    public static final Credentials CREDENTIALS_NONE = new Credentials(null, null);

    public static final String WEB_USERNAME = "securityWebUser";
    public static final String WEB_PASSWORD = "redhat1!";
    public static final Credentials WEB_CREDENTIAL = new Credentials(WEB_USERNAME, WEB_PASSWORD);
    public static final String SECURITY_WEB_ROLE = "securityWebRole";

    public static final String EJB_USERNAME = "securityEjbUser";
    public static final String EJB_PASSWORD = "redhat2!";
    public static final Credentials EJB_CREDENTIAL = new Credentials(EJB_USERNAME, EJB_PASSWORD);
    public static final String SECURITY_EJB_ROLE = "securityEjbRole";

    public static final String SECURITY_DOMAIN = "other";

    private static final String SecuritySLSB1_PACKAGE = "org.jboss.as.test.multinode.security.ejb";
    private static final String SecuritySLSB2_PACKAGE = "org.jboss.as.test.multinode.security.ejb";
    private static final String SecurityWebServlet_CLASS = "org.jboss.as.test.multinode.security.web.SecurityWebClientServlet";

    public static EJBInfo SECURITY_EJB1 = new EJBInfo(null, "SecuritySLSB", "SecuritySLSB", SecuritySLSB1_PACKAGE, SecuritySLSBRemote.class);
    public static EJBInfo SECURITY_EJB2 = new EJBInfo(null, "SecuritySLSB", "SecuritySLSB2", SecuritySLSB2_PACKAGE, SecuritySLSBRemote.class);

    public static ServletInfo SERVLET_1 = new ServletInfo(SecurityWebServlet_CLASS, SECURITY_WEB_ROLE, SecuritySLSBRemote.class.getPackage(), EJBUtil.class.getPackage());

    public static class Credentials {
        String username;
        String password;

        public Credentials(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }
}
