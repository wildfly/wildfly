/*
 * Copyright 2019 Red Hat, Inc.
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
package org.wildfly.test.integration.elytron.realm;

import org.codehaus.plexus.util.StringUtils;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.RolePrintingServlet;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.servlet.AttributePrintingServlet;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Util class for:
 * * AggregateRealmTestCase
 * * AggregateRealmWithTransformerTestCase
 */
public class AggregateRealmUtil {
    public static void assertNoRoleAssigned(URL webAppURL, String username, String password, String queryRoles) throws Exception {
        assertHttpCallEndsBy(webAppURL, username, password, SC_FORBIDDEN, queryRoles);
    }

    public static void assertAuthenticationFailed(URL webAppURL, String username, String password, String queryRoles) throws Exception {
        assertHttpCallEndsBy(webAppURL, username, password, SC_UNAUTHORIZED, queryRoles);
    }
    public static void assertAuthenticationSuccess(URL webAppURL, String username, String password, String queryRoles) throws Exception {
        assertHttpCallEndsBy(webAppURL, username, password, SC_OK, queryRoles);
    }

    public static void assertHttpCallEndsBy(URL webAppURL, String username, String password, int expectedStatusCode, String queryRoles) throws Exception {
        final URL rolesPrintingURL = prepareRolesPrintingURL(webAppURL, queryRoles);
        Utils.makeCallWithBasicAuthn(rolesPrintingURL, username, password, expectedStatusCode);
    }

    public static URL prepareRolesPrintingURL(URL webAppURL, String queryRoles) throws MalformedURLException {
        return new URL(webAppURL.toExternalForm() + RolePrintingServlet.SERVLET_PATH.substring(1) + "?" + queryRoles);
    }


    public static Properties getAttributes(URL webAppURL, final String identity, final String password) throws Exception {
        URL adjustedUrl = new URL(webAppURL.toExternalForm() + AttributePrintingServlet.SERVLET_PATH.substring(1));
        final String attributesResponse = Utils.makeCallWithBasicAuthn(adjustedUrl, identity, password, SC_OK);

        Properties properties = new Properties();
        properties.load(new ByteArrayInputStream(attributesResponse.getBytes()));

        return properties;
    }

    public static void assertAttribute(Properties properties, String attributeName, String... attributeValues) {
        assertTrue("Attribute Exists", properties.containsKey(attributeName));
        List<String> values = Arrays.asList(properties.getProperty(attributeName).split(","));
        assertEquals("Value Count", attributeValues.length, values.size());
        for (String currentValue : attributeValues) {
            assertTrue("Value Exists", values.contains(currentValue));
        }
    }


    public static void assertInRole(final String rolePrintResponse, String role) {
        if (!StringUtils.contains(rolePrintResponse, "," + role + ",")) {
            fail("Missing role '" + role + "' assignment");
        }
    }

    public static void assertNotInRole(final String rolePrintResponse, String role) {
        if (StringUtils.contains(rolePrintResponse, "," + role + ",")) {
            fail("Unexpected role '" + role + "' assignment");
        }
    }

    public static class CustomFSAttributes implements ConfigurableElement {
        private final String realm;
        private final String identity;
        private final String attributeName;
        private final String[] values;

        CustomFSAttributes(String realm, String identity, String attributeName, String... values) {
            this.realm = realm;
            this.identity = identity;
            this.attributeName = attributeName;
            this.values = values;
        }

        @Override
        public String getName() {
            return String.format("Attribute '$s' for identity '%s' in realm '%s'", attributeName, identity, realm);
        }

        @Override
        public void create(CLIWrapper cli) throws Exception {
            cli.sendLine(String.format(
                    "/subsystem=elytron/filesystem-realm=%s:add-identity-attribute(identity=%s, name=%s, value=[%s])", realm,
                    identity, attributeName, String.join(",", values)));
        }

        @Override
        public void remove(CLIWrapper cli) throws Exception {
            // empty, attributes are not stored in management model
        }
    }
}
