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
package org.jboss.as.test.integration.security.loginmodules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainStackServerSetupTask;
import org.jboss.as.test.integration.security.common.LDAPServerSetupTask;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.loginmodules.common.servlets.PrincipalPrintingServlet;
import org.jboss.as.test.integration.security.loginmodules.common.servlets.RolePrintingServlet;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A LdapLoginModuleTestCase, based on examples from https://community.jboss.org/wiki/LdapExtLoginModule
 * 
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup({ LdapExtLoginModuleTestCase.LDAPSetup.class, LdapExtLoginModuleTestCase.SecurityDomainSetup1.class,
        LdapExtLoginModuleTestCase.SecurityDomainSetup2.class, LdapExtLoginModuleTestCase.SecurityDomainSetup3.class,
        LdapExtLoginModuleTestCase.SecurityDomainSetup4.class })
@RunAsClient
public class LdapExtLoginModuleTestCase {

    /** The SECURITY_DOMAIN_NAME_PREFIX */
    public static final String SECURITY_DOMAIN_NAME_PREFIX = "test-";

    private static Logger LOGGER = Logger.getLogger(LdapExtLoginModuleTestCase.class);

    private static int LDAP_PORT = 1389;

    private static final String SECURITY_CREDENTIALS = "secret";
    private static final String SECURITY_PRINCIPAL = "uid=admin,ou=system";

    private static final String DEP1 = "DEP1";
    private static final String DEP2 = "DEP2";
    private static final String DEP3 = "DEP3";
    private static final String DEP4 = "DEP4";

    private static final String[] ROLE_NAMES = { "TheDuke", "Echo", "TheDuke2", "Echo2", "JBossAdmin", "jduke", "jduke2",
            "RG1", "RG2", "RG3", "R1", "R2", "R3", "R4", "R5", "Roles" };

    private static final String QUERY_ROLES;
    static {
        final List<NameValuePair> qparams = new ArrayList<NameValuePair>();
        for (final String role : ROLE_NAMES) {
            qparams.add(new BasicNameValuePair(RolePrintingServlet.PARAM_ROLE_NAME, role));
        }
        QUERY_ROLES = URLEncodedUtils.format(qparams, "UTF-8");
    }

    // Public methods --------------------------------------------------------

    /**
     * Creates {@link WebArchive} for {@link #test1(URL)}.
     * 
     * @return
     */
    @Deployment(name = DEP1)
    public static WebArchive deployment1() {
        return createWar(SecurityDomainSetup1.SECURITY_DOMAIN_NAME);
    }

    /**
     * Creates {@link WebArchive} for {@link #test2(URL)}.
     * 
     * @return
     */
    @Deployment(name = DEP2)
    public static WebArchive deployment2() {
        return createWar(SecurityDomainSetup2.SECURITY_DOMAIN_NAME);
    }

    /**
     * Creates {@link WebArchive} for {@link #test3(URL)}.
     * 
     * @return
     */
    @Deployment(name = DEP3)
    public static WebArchive deployment3() {
        return createWar(SecurityDomainSetup3.SECURITY_DOMAIN_NAME);
    }

    /**
     * Creates {@link WebArchive} for {@link #test3(URL)}.
     * 
     * @return
     */
    @Deployment(name = DEP4)
    public static WebArchive deployment4() {
        return createWar(SecurityDomainSetup4.SECURITY_DOMAIN_NAME);
    }

    /**
     * Test case for Example 1.
     * 
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP1)
    public void test1(@ArquillianResource URL webAppURL) throws Exception {
        testDeployment(webAppURL, "jduke");
    }

    /**
     * Test case for Example 2.
     * 
     * @throws Exception
     */
    @Test
    @Ignore("JBPAPP-8556 - LdapExtLoginModule adds the role name(s) also from the mapping object")
    @OperateOnDeployment(DEP2)
    public void test2(@ArquillianResource URL webAppURL) throws Exception {
        testDeployment(webAppURL, "jduke");
    }

    /**
     * Test case for Example 3.
     * 
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP3)
    public void test3(@ArquillianResource URL webAppURL) throws Exception {
        testDeployment(webAppURL, "Java Duke");
    }

    /**
     * Test case for Example 4.
     * 
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP4)
    public void test4(@ArquillianResource URL webAppURL) throws Exception {
        final URL rolesPrintingURL = new URL(webAppURL.toExternalForm() + RolePrintingServlet.SERVLET_PATH + "?" + QUERY_ROLES);
        final String userName = "Java Duke";
        final String rolesResponse = getResponse(rolesPrintingURL, userName, "theduke", 200);

        assertNotInRole(rolesResponse, "jduke");
        assertNotInRole(rolesResponse, "Java Duke");
        assertNotInRole(rolesResponse, "Roles");
        assertNotInRole(rolesResponse, "JBossAdmin");
        assertNotInRole(rolesResponse, "R4");
        //assigned roles
        assertInRole(rolesResponse, "RG2");
        assertInRole(rolesResponse, "R1");
        assertInRole(rolesResponse, "R2");
        assertInRole(rolesResponse, "R3");
        assertInRole(rolesResponse, "R5");

        final URL principalPrintingURL = new URL(webAppURL.toExternalForm() + PrincipalPrintingServlet.SERVLET_PATH + "?"
                + QUERY_ROLES);
        final String principal = getResponse(principalPrintingURL, userName, "theduke", 200);
        assertEquals("Unexpected Principal name", userName, principal);
    }

    /**
     * Returns response body to the given URL as a String. It also checks if the returned HTTP status code is the expected one.
     * If the server returns {@link HttpServletResponse#SC_UNAUTHORIZED}, then a new request is created with the provided
     * credentials.
     * 
     * @param url
     * @param user
     * @param pass
     * @param expectedStatusCode
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     */
    public String getResponse(URL url, String user, String pass, int expectedStatusCode) throws ClientProtocolException,
            IOException, URISyntaxException {
        LOGGER.info("Requesting URL " + url);
        final DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            final HttpGet httpGet = new HttpGet(url.toURI());
            HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            if (HttpServletResponse.SC_UNAUTHORIZED != statusCode) {
                assertEquals("Unexpected HTTP response status code.", expectedStatusCode, statusCode);
                return EntityUtils.toString(response.getEntity());
            }
            HttpEntity entity = response.getEntity();
            if (entity != null)
                EntityUtils.consume(entity);

            final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user, pass);
            httpClient.getCredentialsProvider().setCredentials(new AuthScope(url.getHost(), url.getPort()), credentials);

            response = httpClient.execute(httpGet);
            statusCode = response.getStatusLine().getStatusCode();
            assertEquals("Unexpected status code returned after the authentication.", expectedStatusCode, statusCode);
            return EntityUtils.toString(response.getEntity());
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpClient.getConnectionManager().shutdown();
        }
    }

    // Private methods -------------------------------------------------------

    /**
     * Tests role assignment.
     * 
     * @param webAppURL
     * @throws MalformedURLException
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     */
    private void testDeployment(URL webAppURL, String username) throws MalformedURLException, ClientProtocolException,
            IOException, URISyntaxException {
        final URL rolesPrintingURL = new URL(webAppURL.toExternalForm() + RolePrintingServlet.SERVLET_PATH + "?" + QUERY_ROLES);
        final String rolesResponse = getResponse(rolesPrintingURL, username, "theduke", 200);

        assertNotInRole(rolesResponse, "jduke");
        assertNotInRole(rolesResponse, "Java Duke");
        assertNotInRole(rolesResponse, "Roles");
        assertNotInRole(rolesResponse, "JBossAdmin");
        assertNotInRole(rolesResponse, "TheDuke2");
        assertNotInRole(rolesResponse, "Echo2");

        //assigned roles
        assertInRole(rolesResponse, "TheDuke");
        assertInRole(rolesResponse, "Echo");

        final URL principalPrintingURL = new URL(webAppURL.toExternalForm() + PrincipalPrintingServlet.SERVLET_PATH + "?"
                + QUERY_ROLES);
        final String principal = getResponse(principalPrintingURL, username, "theduke", 200);
        assertEquals("Unexpected Principal name", username, principal);
    }

    /**
     * Creates a {@link WebArchive} for given security domain.
     * 
     * @param securityDomainName
     * @return
     */
    private static WebArchive createWar(String securityDomainName) {
        LOGGER.info("Start deployment for security-domain " + securityDomainName);
        final WebArchive war = ShrinkWrap.create(WebArchive.class, securityDomainName + ".war");
        war.addClasses(RolePrintingServlet.class, PrincipalPrintingServlet.class);
        war.addAsWebInfResource(LdapExtLoginModuleTestCase.class.getPackage(), LdapExtLoginModuleTestCase.class.getSimpleName()
                + "-web.xml", "web.xml");
        war.addAsWebInfResource(new StringAsset("<jboss-web><security-domain>" + securityDomainName
                + "</security-domain></jboss-web>"), "jboss-web.xml");
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(war.toString(true));
        }
        return war;
    }

    /**
     * Asserts, the role list returned from the {@link RolePrintingServlet} contains the given role.
     * 
     * @param rolePrintResponse
     * @param role
     */
    private void assertInRole(final String rolePrintResponse, String role) {
        assertTrue("Missing role assignment", StringUtils.contains(rolePrintResponse, "," + role + ","));
    }

    /**
     * Asserts, the role list returned from the {@link RolePrintingServlet} doesn't contain the given role.
     * 
     * @param rolePrintResponse
     * @param role
     */
    private void assertNotInRole(final String rolePrintResponse, String role) {
        assertFalse("Unexpected role assignment", StringUtils.contains(rolePrintResponse, "," + role + ","));
    }

    /**
     * Returns URL of the LDAP server.
     * 
     * @return
     */
    private static String getLDAPProviderUrl(final ManagementClient mgmtClient) {
        return "ldap://" + Utils.getSecondaryTestAddress(mgmtClient) + ":" + LDAP_PORT;
    }

    // Embedded classes ------------------------------------------------------

    /**
     * A LDAPSetup.
     * 
     * @author Josef Cacek
     */
    static class LDAPSetup extends LDAPServerSetupTask {

        /**
         * @see org.jboss.qa.security.ldap.loginmodule.LDAPServerSetupTask#configureLdapServer()
         */
        @Override
        protected void configureLdapServer() throws Exception {
            super.configureLdapServer();
            LOGGER.info("Importing LDIF file.");
            importLdif(LdapExtLoginModuleTestCase.class.getResourceAsStream(LdapExtLoginModuleTestCase.class.getSimpleName()
                    + ".ldif"));
        }

        /**
         * @see org.jboss.qa.security.ldap.loginmodule.LDAPServerSetupTask#configureDirectoryService()
         */
        @Override
        protected void configureDirectoryService() throws Exception {
            super.configureDirectoryService();
            LOGGER.info("Creating 'jboss' DirectoryService partition.");
            addPartition("jboss", "dc=jboss,dc=org");
        }

        /**
         * @see org.jboss.qa.security.ldap.loginmodule.LDAPServerSetupTask#getPort()
         */
        @Override
        protected int getPort() {
            return LDAP_PORT;
        }

    }

    /**
     * A SecurityDomainSetup1.
     */
    static class SecurityDomainSetup1 extends AbstractSecurityDomainStackServerSetupTask {

        protected static final String SECURITY_DOMAIN_NAME = SECURITY_DOMAIN_NAME_PREFIX + DEP1;

        /**
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup#getSecurityDomainName()
         */
        @Override
        protected String getSecurityDomainName() {
            return SECURITY_DOMAIN_NAME;
        }

        /**
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainStackServerSetupTask#getLoginModuleConfigurations()
         */
        @Override
        protected SecurityModuleConfiguration[] getLoginModuleConfigurations() {
            SecurityModuleConfiguration ldapLoginModule = new SecurityModuleConfiguration() {

                public String getName() {
                    return "org.jboss.security.auth.spi.LdapExtLoginModule";
                }

                public String getFlag() {
                    return "sufficient";
                }

                public Map<String, String> getOptions() {
                    Map<String, String> moduleOptions = new HashMap<String, String>();

                    moduleOptions.put("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
                    moduleOptions.put("java.naming.provider.url", getLDAPProviderUrl(managementClient));
                    moduleOptions.put("java.naming.security.authentication", "simple");
                    moduleOptions.put("bindDN", SECURITY_PRINCIPAL);
                    moduleOptions.put("bindCredential", SECURITY_CREDENTIALS);
                    moduleOptions.put("baseCtxDN", "ou=People,dc=jboss,dc=org");
                    moduleOptions.put("baseFilter", "(uid={0})");
                    moduleOptions.put("rolesCtxDN", "ou=Roles,dc=jboss,dc=org");
                    moduleOptions.put("roleFilter", "(member={1})");
                    moduleOptions.put("roleAttributeID", "cn");
                    return moduleOptions;
                }

            };
            return new SecurityModuleConfiguration[] { ldapLoginModule };
        }
    }

    /**
     * A SecurityDomainSetup2.
     */
    static class SecurityDomainSetup2 extends AbstractSecurityDomainStackServerSetupTask {

        protected static final String SECURITY_DOMAIN_NAME = SECURITY_DOMAIN_NAME_PREFIX + DEP2;

        /**
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup#getSecurityDomainName()
         */
        @Override
        protected String getSecurityDomainName() {
            return SECURITY_DOMAIN_NAME;
        }

        /**
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainStackServerSetupTask#getLoginModuleConfigurations()
         */
        @Override
        protected SecurityModuleConfiguration[] getLoginModuleConfigurations() {
            SecurityModuleConfiguration ldapLoginModule = new SecurityModuleConfiguration() {

                public String getName() {
                    return "LdapExtended";
                }

                public String getFlag() {
                    return "sufficient";
                }

                public Map<String, String> getOptions() {
                    Map<String, String> moduleOptions = new HashMap<String, String>();

                    moduleOptions.put("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
                    moduleOptions.put("java.naming.provider.url", getLDAPProviderUrl(managementClient));
                    moduleOptions.put("java.naming.security.authentication", "simple");
                    moduleOptions.put("bindDN", SECURITY_PRINCIPAL);
                    moduleOptions.put("bindCredential", SECURITY_CREDENTIALS);
                    moduleOptions.put("baseCtxDN", "ou=People,o=example2,dc=jboss,dc=org");
                    moduleOptions.put("baseFilter", "(uid={0})");
                    moduleOptions.put("rolesCtxDN", "ou=Roles,o=example2,dc=jboss,dc=org");
                    moduleOptions.put("roleFilter", "(cn={0})");
                    moduleOptions.put("roleAttributeID", "description");
                    moduleOptions.put("roleAttributeIsDN", "true");
                    moduleOptions.put("roleNameAttributeID", "cn");
                    moduleOptions.put("roleRecursion", "0");
                    return moduleOptions;
                }

            };
            return new SecurityModuleConfiguration[] { ldapLoginModule };
        }
    }

    /**
     * A SecurityDomainSetup3.
     */
    static class SecurityDomainSetup3 extends AbstractSecurityDomainStackServerSetupTask {

        protected static final String SECURITY_DOMAIN_NAME = SECURITY_DOMAIN_NAME_PREFIX + DEP3;

        /**
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup#getSecurityDomainName()
         */
        @Override
        protected String getSecurityDomainName() {
            return SECURITY_DOMAIN_NAME;
        }

        /**
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainStackServerSetupTask#getLoginModuleConfigurations()
         */
        @Override
        protected SecurityModuleConfiguration[] getLoginModuleConfigurations() {
            SecurityModuleConfiguration ldapLoginModule = new SecurityModuleConfiguration() {

                public String getName() {
                    return "LdapExtended";
                }

                public String getFlag() {
                    return "sufficient";
                }

                public Map<String, String> getOptions() {
                    Map<String, String> moduleOptions = new HashMap<String, String>();

                    moduleOptions.put("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
                    moduleOptions.put("java.naming.provider.url", getLDAPProviderUrl(managementClient));
                    moduleOptions.put("java.naming.security.authentication", "simple");
                    moduleOptions.put("bindDN", SECURITY_PRINCIPAL);
                    moduleOptions.put("bindCredential", SECURITY_CREDENTIALS);
                    moduleOptions.put("baseCtxDN", "ou=People,o=example3,dc=jboss,dc=org");
                    moduleOptions.put("baseFilter", "(cn={0})");

                    moduleOptions.put("rolesCtxDN", "ou=Roles,o=example3,dc=jboss,dc=org");
                    moduleOptions.put("roleFilter", "(member={1})");
                    moduleOptions.put("roleAttributeID", "cn");
                    moduleOptions.put("roleRecursion", "0");
                    return moduleOptions;
                }

            };
            return new SecurityModuleConfiguration[] { ldapLoginModule };
        }
    }

    /**
     * A SecurityDomainSetup4.
     */
    static class SecurityDomainSetup4 extends AbstractSecurityDomainStackServerSetupTask {

        protected static final String SECURITY_DOMAIN_NAME = SECURITY_DOMAIN_NAME_PREFIX + DEP4;

        /**
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup#getSecurityDomainName()
         */
        @Override
        protected String getSecurityDomainName() {
            return SECURITY_DOMAIN_NAME;
        }

        /**
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainStackServerSetupTask#getLoginModuleConfigurations()
         */
        @Override
        protected SecurityModuleConfiguration[] getLoginModuleConfigurations() {
            SecurityModuleConfiguration ldapLoginModule = new SecurityModuleConfiguration() {

                public String getName() {
                    return "LdapExtended";
                }

                public String getFlag() {
                    return "sufficient";
                }

                public Map<String, String> getOptions() {
                    Map<String, String> moduleOptions = new HashMap<String, String>();

                    moduleOptions.put("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
                    moduleOptions.put("java.naming.provider.url", getLDAPProviderUrl(managementClient));
                    moduleOptions.put("java.naming.security.authentication", "simple");
                    moduleOptions.put("bindDN", SECURITY_PRINCIPAL);
                    moduleOptions.put("bindCredential", SECURITY_CREDENTIALS);

                    moduleOptions.put("baseCtxDN", "ou=People,o=example4,dc=jboss,dc=org");
                    moduleOptions.put("baseFilter", "(cn={0})");

                    moduleOptions.put("rolesCtxDN", "ou=Roles,o=example4,dc=jboss,dc=org");
                    moduleOptions.put("roleFilter", "(member={1})");
                    moduleOptions.put("roleAttributeID", "cn");
                    moduleOptions.put("roleRecursion", "1");
                    return moduleOptions;
                }

            };
            return new SecurityModuleConfiguration[] { ldapLoginModule };
        }
    }

}
