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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.ClientProtocolException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.Coding;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.common.config.SecurityModule.Builder;
import org.jboss.as.test.integration.security.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Tests for UserRoles login module.
 *
 * @author Jan Lanik
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup({UsersRolesLoginModuleTestCase.PropertyFilesSetup.class, UsersRolesLoginModuleTestCase.SecurityDomainsSetup.class})
@RunAsClient
@Category(CommonCriteria.class)
public class UsersRolesLoginModuleTestCase {

    private static Logger LOGGER = Logger.getLogger(UsersRolesLoginModuleTestCase.class);

    private static final String DEP1 = "UsersRoles-externalFiles";
    private static final String DEP2 = "UsersRoles-MD5";
    private static final String DEP3 = "UsersRoles-MD5-hex";
    private static final String DEP4 = "UsersRoles-MD5-base64";
    private static final String DEP5a = "UsersRoles-hashUserPassword-false";
    private static final String DEP5b = "UsersRoles-hashUserPassword-true";
    private static final String DEP6a = "UsersRoles-hashOnlyStorePassword";
    private static final String DEP6b = "UsersRoles-hashStorePassword";
    private static final String DEP7a = "UsersRoles-ignorePasswordCase-true";
    private static final String DEP7b = "UsersRoles-ignorePasswordCase-false";

    private static final String ANIL = "anil";
    private static final String MARCUS = "marcus";
    private static final String ANIL_PWD = "anilPwd";
    private static final String MARCUS_PWD = "marcusPwd";

    private static final String ROLES = ANIL + "=" + SimpleSecuredServlet.ALLOWED_ROLE + "\n" + MARCUS + "=testRole";

    private static final String USERS_EXT = ANIL + "=" + MARCUS_PWD + "\n" + MARCUS + "=" + ANIL_PWD;
    private static final String ROLES_EXT = MARCUS + "=" + SimpleSecuredServlet.ALLOWED_ROLE + "\n" + ANIL + "=testRole";

    /**
     * plaintext login with no additional options
     */
    @Deployment(name = DEP1)
    public static WebArchive appDeployment1() {
        return createWar(DEP1, null);
    }

    /**
     * passwords stored as MD5, no additional options
     */
    @Deployment(name = DEP2)
    public static WebArchive appDeployment2() {
        return createWar(DEP2, Coding.BASE_64);
    }

    /**
     * passwords stored as MD5 in HEX encoding, no additional options
     */
    @Deployment(name = DEP3)
    public static WebArchive appDeployment3() {
        return createWar(DEP3, Coding.HEX);
    }

    /**
     * passwords stored as MD5 in BASE64 encoding, no additional options
     */
    @Deployment(name = DEP4)
    public static WebArchive appDeployment4() {
        return createWar(DEP4, Coding.BASE_64);
    }

    /**
     * tests hashUserPassword=false option
     */
    @Deployment(name = DEP5a)
    public static WebArchive appDeployment5a() {
        return createWar(DEP5a, null);
    }

    /**
     * tests hashUserPassword=true option
     */
    @Deployment(name = DEP5b)
    public static WebArchive appDeployment5b() {
        return createWar(DEP5b, Coding.BASE_64);
    }

    /**
     * tests hashUserPassword option
     */
    @Deployment(name = DEP6a)
    public static WebArchive appDeployment6a() {
        return createWar(DEP6a, null);
    }

    /**
     * tests hashUserPassword option
     */
    @Deployment(name = DEP6b)
    public static WebArchive appDeployment6b() {
        return createWar(DEP6b, null);
    }

    /**
     * tests ignorePasswordCase=true option
     */
    @Deployment(name = DEP7a)
    public static WebArchive appDeployment7a() {
        return createWar(DEP7a, null);
    }

    /**
     * tests ignorePasswordCase=false option
     */
    @Deployment(name = DEP7b)
    public static WebArchive appDeployment7b() {
        return createWar(DEP7b, null);
    }

    /**
     * testExternalFiles
     *
     * @throws Exception
     * @see #USERS_EXT
     * @see #ROLES_EXT
     */
    @OperateOnDeployment(DEP1)
    @Test
    public void testExternalFiles(@ArquillianResource URL url) throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));

        //successful authentication and authorization
        assertEquals("Response body is not correct.", SimpleSecuredServlet.RESPONSE_BODY,
                Utils.makeCallWithBasicAuthn(servletUrl, MARCUS, ANIL_PWD, 200));
        //successful authentication and unsuccessful authorization
        Utils.makeCallWithBasicAuthn(servletUrl, ANIL, MARCUS_PWD, 403);
        //tests related to case insensitiveness
        Utils.makeCallWithBasicAuthn(servletUrl, ANIL, MARCUS_PWD.toUpperCase(Locale.ENGLISH), 401);
        Utils.makeCallWithBasicAuthn(servletUrl, MARCUS, ANIL_PWD.toLowerCase(Locale.ENGLISH), 401);
        //unsuccessful authentication
        Utils.makeCallWithBasicAuthn(servletUrl, MARCUS, MARCUS_PWD, 401);
        Utils.makeCallWithBasicAuthn(servletUrl, ANIL, MARCUS, 401);
        Utils.makeCallWithBasicAuthn(servletUrl, ANIL_PWD, MARCUS_PWD, 401);
        Utils.makeCallWithBasicAuthn(servletUrl, ANIL, Utils.hashMD5(MARCUS_PWD, Coding.BASE_64), 401);
        Utils.makeCallWithBasicAuthn(servletUrl, ANIL, Utils.hashMD5(MARCUS_PWD, Coding.HEX), 401);
    }

    /**
     * testMD5Password
     *
     * @throws Exception
     */
    @OperateOnDeployment(DEP2)
    @Test
    public void testMD5Password(@ArquillianResource URL url) throws Exception {
        testAccess(url, false);
    }

    /**
     * testMD5PasswordHex
     *
     * @throws Exception
     */
    @OperateOnDeployment(DEP3)
    @Test
    public void testMD5PasswordHex(@ArquillianResource URL url) throws Exception {
        testAccess(url, false);
    }

    /**
     * testMD5PasswordBase64
     *
     * @throws Exception
     */
    @OperateOnDeployment(DEP4)
    @Test
    public void testMD5PasswordBase64(@ArquillianResource URL url) throws Exception {
        testAccess(url, false);
    }

    /**
     * testHashUserPasswordTrue
     *
     * @throws Exception
     */
    @OperateOnDeployment(DEP5a)
    @Test
    public void testHashUserPasswordTrue(@ArquillianResource URL url) throws Exception {
        testAccess(url, false);
    }

    /**
     * testHashUserPassword
     *
     * @throws Exception
     */
    @OperateOnDeployment(DEP5b)
    @Test
    public void testHashUserPasswordFalse(@ArquillianResource URL url) throws Exception {
        testAccess(url, false);
    }

    /**
     * testHashOnlyStorePassword
     *
     * @throws Exception
     */
    @OperateOnDeployment(DEP6a)
    @Test
    public void testHashOnlyStorePassword(@ArquillianResource URL url) throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));
        //successful authentication and authorization
        assertEquals("Response body is not correct.", SimpleSecuredServlet.RESPONSE_BODY,
                Utils.makeCallWithBasicAuthn(servletUrl, ANIL, Utils.hashMD5(ANIL_PWD, Coding.BASE_64), 200));
        //successful authentication and unsuccessful authorization
        Utils.makeCallWithBasicAuthn(servletUrl, MARCUS, Utils.hashMD5(MARCUS_PWD, Coding.BASE_64), 403);
        //unsuccessful authentication
        Utils.makeCallWithBasicAuthn(servletUrl, ANIL, ANIL_PWD, 401);
        Utils.makeCallWithBasicAuthn(servletUrl, MARCUS, MARCUS_PWD, 401);
    }

    /**
     * testHashStorePassword
     *
     * @throws Exception
     */
    @OperateOnDeployment(DEP6b)
    @Test
    public void testHashStorePassword(@ArquillianResource URL url) throws Exception {
        testAccess(url, false);
    }

    /**
     * testIgnorePasswordCaseTrue
     *
     * @throws Exception
     */
    @OperateOnDeployment(DEP7a)
    @Test
    public void testIgnorePasswordCaseTrue(@ArquillianResource URL url) throws Exception {
        testAccess(url, true);
    }

    /**
     * testIgnorePasswordCaseFalse
     *
     * @throws Exception
     */
    @OperateOnDeployment(DEP7b)
    @Test
    public void testIgnorePasswordCaseFalse(@ArquillianResource URL url) throws Exception {
        testAccess(url, false);
    }

    // Private methods -------------------------------------------------------

    /**
     * Tests access to a protected servlet.
     *
     * @param url
     * @param ignoreCase flag which says if the password should be case insensitive
     * @throws MalformedURLException
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     */
    private void testAccess(URL url, boolean ignoreCase) throws IOException,
            URISyntaxException {
        final URL servletUrl = new URL(url.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));
        //successful authentication and authorization
        assertEquals("Response body is not correct.", SimpleSecuredServlet.RESPONSE_BODY,
                Utils.makeCallWithBasicAuthn(servletUrl, ANIL, ANIL_PWD, 200));
        //successful authentication and unsuccessful authorization
        Utils.makeCallWithBasicAuthn(servletUrl, MARCUS, MARCUS_PWD, 403);
        //tests related to case (in)sensitiveness
        if (ignoreCase) {
            assertEquals("Response body is not correct.", SimpleSecuredServlet.RESPONSE_BODY,
                    Utils.makeCallWithBasicAuthn(servletUrl, ANIL, ANIL_PWD.toUpperCase(Locale.ENGLISH), 200));
            Utils.makeCallWithBasicAuthn(servletUrl, MARCUS, MARCUS_PWD.toLowerCase(Locale.ENGLISH), 403);
        } else {
            Utils.makeCallWithBasicAuthn(servletUrl, ANIL, ANIL_PWD.toUpperCase(Locale.ENGLISH), 401);
            Utils.makeCallWithBasicAuthn(servletUrl, MARCUS, MARCUS_PWD.toLowerCase(Locale.ENGLISH), 401);
        }
        //unsuccessful authentication
        Utils.makeCallWithBasicAuthn(servletUrl, ANIL, MARCUS_PWD, 401);
        Utils.makeCallWithBasicAuthn(servletUrl, ANIL, MARCUS, 401);
        Utils.makeCallWithBasicAuthn(servletUrl, MARCUS_PWD, MARCUS, 401);
        Utils.makeCallWithBasicAuthn(servletUrl, ANIL, Utils.hashMD5(ANIL, Coding.BASE_64), 401);
        Utils.makeCallWithBasicAuthn(servletUrl, ANIL, Utils.hashMD5(ANIL, Coding.HEX), 401);
    }

    /**
     * Creates {@link WebArchive} (WAR) for given deployment name.
     *
     * @param deployment
     * @return
     */
    private static WebArchive createWar(final String deployment, Coding coding) {
        LOGGER.trace("Starting deployment " + deployment);

        final String users = ANIL + "=" + Utils.hashMD5(ANIL_PWD, coding) + "\n" + MARCUS + "="
                + Utils.hashMD5(MARCUS_PWD, coding);

        final WebArchive war = ShrinkWrap.create(WebArchive.class, deployment + ".war");
        war.addClasses(SimpleSecuredServlet.class, SimpleServlet.class);
        war.addAsWebInfResource(UsersRolesLoginModuleTestCase.class.getPackage(), "web-basic-authn.xml", "web.xml");
        war.addAsResource(new StringAsset(users), "users.properties");
        war.addAsResource(new StringAsset(ROLES), "roles.properties");
        war.addAsWebInfResource(new StringAsset("<jboss-web>" + //
                "<security-domain>" + deployment + "</security-domain>" + //
                "</jboss-web>"), "jboss-web.xml");

        return war;
    }

    // Embedded classes ------------------------------------------------------

    /**
     * A {@link ServerSetupTask} instance which creates security domains for this test case.
     *
     * @author Josef Cacek
     */
    static class SecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {

        /**
         * Returns SecurityDomains configuration for this testcase.
         *
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask#getSecurityDomains()
         */
        @Override
        protected SecurityDomain[] getSecurityDomains() {

            final Map<String, String> lmOptions = new HashMap<String, String>();
            final Builder loginModuleBuilder = new SecurityModule.Builder().name("UsersRoles").options(lmOptions);

            lmOptions.put("usersProperties", PropertyFilesSetup.FILE_USERS.getAbsolutePath());
            lmOptions.put("rolesProperties", PropertyFilesSetup.FILE_ROLES.getAbsolutePath());
            final SecurityDomain sd1 = new SecurityDomain.Builder().name(DEP1).loginModules(loginModuleBuilder.build()).build();

            lmOptions.remove("usersProperties");
            lmOptions.remove("rolesProperties");
            lmOptions.put("hashAlgorithm", "MD5");
            final SecurityDomain sd2 = new SecurityDomain.Builder().name(DEP2).loginModules(loginModuleBuilder.build()).build();

            lmOptions.put("hashEncoding", "hex");
            final SecurityDomain sd3 = new SecurityDomain.Builder().name(DEP3).loginModules(loginModuleBuilder.build()).build();

            lmOptions.put("hashEncoding", "base64");
            final SecurityDomain sd4 = new SecurityDomain.Builder().name(DEP4).loginModules(loginModuleBuilder.build()).build();

            lmOptions.remove("hashEncoding");
            lmOptions.put("hashUserPassword", "false");
            final SecurityDomain sd5a = new SecurityDomain.Builder().name(DEP5a).loginModules(loginModuleBuilder.build())
                    .build();

            lmOptions.put("hashUserPassword", "true");
            final SecurityDomain sd5b = new SecurityDomain.Builder().name(DEP5b).loginModules(loginModuleBuilder.build())
                    .build();

            lmOptions.put("hashUserPassword", "false");
            lmOptions.put("hashStorePassword", "true");
            final SecurityDomain sd6a = new SecurityDomain.Builder().name(DEP6a).loginModules(loginModuleBuilder.build())
                    .build();

            lmOptions.remove("hashUserPassword");
            final SecurityDomain sd6b = new SecurityDomain.Builder().name(DEP6b).loginModules(loginModuleBuilder.build())
                    .build();

            lmOptions.remove("hashStorePassword");
            lmOptions.remove("hashAlgorithm");
            lmOptions.put("ignorePasswordCase", "true");
            final SecurityDomain sd7a = new SecurityDomain.Builder().name(DEP7a).loginModules(loginModuleBuilder.build())
                    .build();

            lmOptions.put("ignorePasswordCase", "false");
            final SecurityDomain sd7b = new SecurityDomain.Builder().name(DEP7b).loginModules(loginModuleBuilder.build())
                    .build();

            return new SecurityDomain[]{sd1, sd2, sd3, sd4, sd5a, sd5b, sd6a, sd6b, sd7a, sd7b};
        }
    }

    /**
     * A {@link ServerSetupTask} instance which creates property files with users and roles.
     *
     * @author Josef Cacek
     */
    static class PropertyFilesSetup implements ServerSetupTask {

        public static final File FILE_USERS = new File("test-users.properties");
        public static final File FILE_ROLES = new File("test-roles.properties");

        /**
         * Generates property files.
         */
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            FileUtils.writeStringToFile(FILE_USERS, USERS_EXT, "ISO-8859-1");
            FileUtils.writeStringToFile(FILE_ROLES, ROLES_EXT, "ISO-8859-1");
        }

        /**
         * Removes generated property files.
         */
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            FILE_USERS.delete();
            FILE_ROLES.delete();
        }
    }

}
