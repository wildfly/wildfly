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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.security.common.AbstractLoginModuleStackServerSetupTask;
import org.jboss.as.test.integration.security.common.LDAPServerSetupTask;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.loginmodules.common.servlets.AbstractLoginModuleTestServlet;
import org.jboss.as.test.integration.security.loginmodules.common.servlets.SecuredLdapLoginModuleTestServlet;
import org.jboss.as.test.integration.security.loginmodules.common.servlets.SimpleUnsecuredServlet;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A LdapLoginModuleTestCase.
 * 
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup({ LdapLoginModuleTestCase.LDAPSetup.class, LdapLoginModuleTestCase.SecurityDomainSetup.class })
@RunAsClient
public class LdapLoginModuleTestCase {

    private static Logger LOGGER = Logger.getLogger(LdapLoginModuleTestCase.class);

    private static String SECURITY_DOMAIN_NAME = "LDAP-test";

    private static int LDAP_PORT = 1389;

    private static final String INITIAL_CONTEXT_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
    private static final String SEARCH_BASE = "dc=jboss,dc=org";
    private static final String SECURITY_AUTHENTICATION = "simple";
    private static final String SECURITY_CREDENTIALS = "secret";
    private static final String SECURITY_PRINCIPAL = "uid=admin,ou=system";

    @ArquillianResource
    URL webAppURL;

    @ArquillianResource
    ManagementClient mgmtClient;

    // Public methods --------------------------------------------------------

    /**
     * Creates {@link WebArchive} with the {@link OKServlet}.
     * 
     * @return
     * @throws SQLException
     */
    @Deployment
    public static WebArchive deployment() {
        LOGGER.info("start deployment");
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "ldap-login-module.war");
        war.addClasses(SecuredLdapLoginModuleTestServlet.class, SimpleUnsecuredServlet.class,
                AbstractLoginModuleTestServlet.class);
        war.addAsWebInfResource(LdapLoginModuleTestCase.class.getPackage(), "web-basic-authn.xml", "web.xml");
        war.addAsWebInfResource(new StringAsset("<jboss-web><security-domain>" + SECURITY_DOMAIN_NAME
                + "</security-domain></jboss-web>"), "jboss-web.xml");
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(war.toString(true));
        }
        return war;
    }

    /**
     * Correct login
     * 
     * @throws Exception
     */
    @Test
    public void testSuccesfullAuth() throws Exception {
        final String urlAsString = webAppURL.toExternalForm() + SecuredLdapLoginModuleTestServlet.SERVLET_PATH;
        LOGGER.info("Testing successfull authentication - " + urlAsString);
        final String responseBody = getResponse(urlAsString, "jduke", "theduke", 200);
        assertEquals("Expected response body doesn't match the returned one.", AbstractLoginModuleTestServlet.RESPONSE_BODY,
                responseBody);
    }

    /**
     * Incorrect login
     * 
     * @throws Exception
     */
    @Test
    public void testUnsucessfulAuthn() throws Exception {
        final String urlAsString = webAppURL.toExternalForm() + SecuredLdapLoginModuleTestServlet.SERVLET_PATH;
        LOGGER.info("Testing failed authentication - " + urlAsString);
        getResponse(urlAsString, "anil", "theduke", 401);
        getResponse(urlAsString, "jduke", "anil", 401);
        getResponse(urlAsString, "anil", "anil", 401);
    }

    /**
     * Correct login, but without permissions.
     * 
     * @throws Exception
     */
    @Test
    public void testUnsucessfulAuthz() throws Exception {
        final String urlAsString = webAppURL.toExternalForm() + SecuredLdapLoginModuleTestServlet.SERVLET_PATH;
        LOGGER.info("Testing failed authorization - " + urlAsString);
        getResponse(urlAsString, "tester", "password", 403);
    }

    /**
     * Incorrect login
     * 
     * @throws Exception
     */
    @Test
    public void testUnsecured() throws Exception {
        final String urlAsString = webAppURL.toExternalForm() + SimpleUnsecuredServlet.SERVLET_PATH;
        LOGGER.info("Testing access to unprotected resource - " + urlAsString);
        final String responseBody = getResponse(urlAsString, null, null, 200);
        assertEquals("Expected response body doesn't match the returned one.", AbstractLoginModuleTestServlet.RESPONSE_BODY,
                responseBody);
    }

    /**
     * Tests ability to connect to a LDAP server.
     * 
     * @throws NamingException
     */
    @Test
    public void testLDAPReady() throws NamingException {
        final DirContext ctx = createDirContext();

        final String searchBase = SEARCH_BASE;
        final SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        final String filter = "(objectClass=person)";
        assertTrue(ctx.search(searchBase, filter, sc).hasMore());
        ctx.close();
    }

    /**
     * Returns initialized DirContext.
     * 
     * @return initialized {@link DirContext}
     * @throws NamingException if creating of {@link DirContext} fails
     */
    private DirContext createDirContext() throws NamingException {
        final Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
        env.put(Context.PROVIDER_URL, getLDAPProviderUrl(mgmtClient));
        env.put(Context.SECURITY_AUTHENTICATION, SECURITY_AUTHENTICATION);
        env.put(Context.SECURITY_PRINCIPAL, SECURITY_PRINCIPAL);

        if (LOGGER.isDebugEnabled()) {
            env.put(Context.SECURITY_CREDENTIALS, "***");
            LOGGER.debug("Creating InitialDirContext: " + env);
        }

        env.put(Context.SECURITY_CREDENTIALS, SECURITY_CREDENTIALS);
        final DirContext ctx = new InitialDirContext(env);
        return ctx;
    }

    /**
     * Returns URL of the LDAP server.
     * 
     * @return
     */
    private static String getLDAPProviderUrl(final ManagementClient mgmtClient) {
        return "ldap://" + Utils.getSecondaryTestAddress(mgmtClient) + ":" + LDAP_PORT;
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
     */
    public String getResponse(String url, String user, String pass, int expectedStatusCode) throws ClientProtocolException,
            IOException {
        final DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            final HttpGet httpget = new HttpGet(url);

            HttpResponse response = httpClient.execute(httpget);
            int statusCode = response.getStatusLine().getStatusCode();
            if (HttpServletResponse.SC_UNAUTHORIZED != statusCode) {
                assertEquals("Unexpected HTTP response status code.", expectedStatusCode, statusCode);
                return EntityUtils.toString(response.getEntity());
            }
            HttpEntity entity = response.getEntity();
            if (entity != null)
                EntityUtils.consume(entity);

            final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user, pass);
            httpClient.getCredentialsProvider().setCredentials(new AuthScope(webAppURL.getHost(), webAppURL.getPort()),
                    credentials);

            response = httpClient.execute(httpget);
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
            importLdif(LdapLoginModuleTestCase.class.getResourceAsStream("LdapLoginModuleTestCase.ldif"));
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
     * A SecurityDomainSetup.
     */
    static class SecurityDomainSetup extends AbstractLoginModuleStackServerSetupTask {

        /**
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup#getSecurityDomainName()
         */
        @Override
        protected String getSecurityDomainName() {
            return SECURITY_DOMAIN_NAME;
        }

        /**
         * @see org.jboss.as.test.integration.security.common.AbstractLoginModuleStackServerSetupTask#getLoginModuleConfigurations()
         */
        @Override
        protected LoginModuleConfiguration[] getLoginModuleConfigurations() {
            LoginModuleConfiguration ldapLoginModule = new LoginModuleConfiguration() {

                public String getName() {
                    return "Ldap";
                }

                public String getFlag() {
                    return "sufficient";
                }

                public Map<String, String> getOptions() {
                    Map<String, String> moduleOptions = new HashMap<String, String>();

                    //InitialContextFactory implementation class name. This defaults to the Sun LDAP provider implementation com.sun.jndi.ldap.LdapCtxFactory.
                    moduleOptions.put("java.naming.factory.initial", INITIAL_CONTEXT_FACTORY);

                    //LDAP URL for the LDAP server.
                    moduleOptions.put("java.naming.provider.url", getLDAPProviderUrl(managementClient));

                    //Security level to use. This defaults to simple.
                    moduleOptions.put("java.naming.security.authentication", SECURITY_AUTHENTICATION);

                    //Transport protocol to use for secure access, such as, SSL.
                    //            moduleOptions.put("java.naming.security.protocol","");

                    //Principal for authenticating the caller to the service. This is built from other properties as described below.
                    moduleOptions.put("java.naming.security.principal", SECURITY_PRINCIPAL);

                    //Authentication scheme to use. For example, hashed password, clear-text password, key, certificate, and so on.
                    moduleOptions.put("java.naming.security.credentials", SECURITY_CREDENTIALS);

                    //Prefix added to the username to form the user distinguished name. See principalDNSuffix for more info.
                    moduleOptions.put("principalDNPrefix", "uid=");

                    //Suffix added to the username when forming the user distinguished name. This is useful if you prompt a user for a username and you don't want the user to have to enter the fully distinguished name. Using this property and principalDNSuffix the userDN will be formed as principalDNPrefix + username + principalDNSuffix
                    moduleOptions.put("principalDNSuffix", ",ou=People,dc=jboss,dc=org");

                    //Value that indicates the credential should be obtained as an opaque Object using the org.jboss.security.auth.callback.ObjectCallback type of Callback rather than as a char[] password using a JAAS PasswordCallback. This allows for passing non-char[] credential information to the LDAP server. The available values are true and false.
                    //            moduleOptions.put("useObjectCredential","");

                    //Fixed, distinguished name to the context to search for user roles.
                    moduleOptions.put("rolesCtxDN", "ou=Roles,dc=jboss,dc=org");

                    //Name of an attribute in the user object that contains the distinguished name to the context to search for user roles. This differs from rolesCtxDN in that the context to search for a user's roles can be unique for each user.
                    //            moduleOptions.put("userRolesCtxDNAttributeName","");

                    //Name of the attribute containing the user roles. If not specified, this defaults to roles.
                    //            moduleOptions.put("roleAttributeID","");            

                    // Flag indicating whether the roleAttributeID contains the fully distinguished name of a role object, or the role name. The role name is taken from the value of the roleNameAttributeId attribute of the context name by the distinguished name.
                    //If true, the role attribute represents the distinguished name of a role object. If false, the role name is taken from the value of roleAttributeID. The default is false.
                    //Note: In certain directory schemas (e.g., MS ActiveDirectory), role attributes in the user object are stored as DNs to role objects instead of simple names. For implementations that use this schema type, roleAttributeIsDN must be set to true.
                    moduleOptions.put("roleAttributeIsDN", "false");

                    // Name of the attribute containing the user roles. If not specified, this defaults to roles.
                    moduleOptions.put("roleAttributeID", "cn");

                    //Name of the attribute in the object containing the user roles that corresponds to the userid. This is used to locate the user roles. If not specified this defaults to uid.
                    moduleOptions.put("uidAttributeID", "member");

                    //Flag that specifies whether the search for user roles should match on the user's fully distinguished name. If true, the full userDN is used as the match value. If false, only the username is used as the match value against the uidAttributeName attribute. The default value is false.
                    moduleOptions.put("matchOnUserDN", "true");

                    //A flag indicating if empty (length 0) passwords should be passed to the LDAP server. An empty password is treated as an anonymous login by some LDAP servers and this may not be a desirable feature. To reject empty passwords, set this to false. If set to true, the LDAP server will validate the empty password. The default is true.
                    //            moduleOptions.put("allowEmptyPasswords","");
                    return moduleOptions;
                }

            };
            return new LoginModuleConfiguration[] { ldapLoginModule };
        }
    }
}
