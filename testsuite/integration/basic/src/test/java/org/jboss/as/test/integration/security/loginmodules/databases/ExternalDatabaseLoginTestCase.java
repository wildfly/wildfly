/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.security.loginmodules.databases;

import static org.jboss.as.test.integration.security.loginmodules.databases.DatabaseCreatorBean.COLUMN_PASSWORD;
import static org.jboss.as.test.integration.security.loginmodules.databases.DatabaseCreatorBean.COLUMN_ROLE;
import static org.jboss.as.test.integration.security.loginmodules.databases.DatabaseCreatorBean.COLUMN_ROLE_GROUP;
import static org.jboss.as.test.integration.security.loginmodules.databases.DatabaseCreatorBean.COLUMN_USER;
import static org.jboss.as.test.integration.security.loginmodules.databases.DatabaseCreatorBean.DS_JNDI;
import static org.jboss.as.test.integration.security.loginmodules.databases.DatabaseCreatorBean.TABLE_NAME_ROLES;
import static org.jboss.as.test.integration.security.loginmodules.databases.DatabaseCreatorBean.TABLE_NAME_USERS;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.Coding;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Tests for {@link org.jboss.security.auth.spi.DatabaseServerLoginModule}. It
 * uses ExampleDS, which is by default embedded H2 database, but this will be
 * replaced with certified databases during QE testing. Database is filled with
 * users and roles on startup with help of DatabaseCreatorBean.
 *
 * @author Filip Bogyai
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup({ExternalDatabaseLoginTestCase.SecurityDomainsSetup.class})
@RunAsClient
@Category(CommonCriteria.class)
public class ExternalDatabaseLoginTestCase {

    private static Logger LOGGER = Logger.getLogger(ExternalDatabaseLoginTestCase.class);

    private static final String MARCUS = "marcus";
    private static final String ANIL = "anil";

    private static final String DB = "DB";

    private static final String MD5 = "MD5";

    /**
     * Creates WAR for test database login module with ExampleDS database
     *
     * @return
     */
    @Deployment(name = DB)
    public static WebArchive appDeployment1() {
        return createWar(DB);
    }

    @OperateOnDeployment(DB)
    @Test
    public void testDefault(@ArquillianResource URL url) throws Exception {
        testAccess(url);
    }

    /**
     * Tests access to a protected servlet.
     *
     * @param url
     * @throws MalformedURLException
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     * @test.objective Test whether user with right name and password is
     * correctly authenticated and authorized. Also test
     * unsuccessful authentication and authorization.
     * <p>
     * It uses web.xml and jboss-web.xml files.
     * @test.expectedResult All asserts are correct and test finishes without
     * any exception.
     */
    private void testAccess(URL url) throws IOException, URISyntaxException {
        final URL servletUrl = new URL(url.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));
        // successful authentication and authorization
        assertEquals("Response body is not correct.", SimpleSecuredServlet.RESPONSE_BODY,
                Utils.makeCallWithBasicAuthn(servletUrl, ANIL, ANIL, 200));
        // successful authentication and unsuccessful authorization
        Utils.makeCallWithBasicAuthn(servletUrl, MARCUS, MARCUS, 403);
        // unsuccessful authentication
        Utils.makeCallWithBasicAuthn(servletUrl, ANIL, MARCUS, 401);
        Utils.makeCallWithBasicAuthn(servletUrl, ANIL, Utils.hash(ANIL, MD5, Coding.BASE_64), 401);
        Utils.makeCallWithBasicAuthn(servletUrl, ANIL, Utils.hash(ANIL, MD5, Coding.HEX), 401);
    }

    /**
     * Creates {@link WebArchive} (WAR) for given deployment name.
     *
     * @param String deployment name
     * @return WebArchive deployment
     */
    private static WebArchive createWar(final String deployment) {
        LOGGER.trace("Starting deployment " + deployment);

        final WebArchive war = ShrinkWrap.create(WebArchive.class, deployment + ".war");
        war.addClasses(SimpleSecuredServlet.class, SimpleServlet.class, DatabaseCreatorBean.class);
        war.addAsWebInfResource(ExternalDatabaseLoginTestCase.class.getPackage(), "web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(deployment), "jboss-web.xml");

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
            final SecurityModule.Builder loginModuleBuilder = new SecurityModule.Builder().name("Database").options(
                    getLoginModuleOptions());
            final SecurityDomain sd1 = new SecurityDomain.Builder().name(DB).loginModules(loginModuleBuilder.build()).build();
            return new SecurityDomain[]{sd1};
        }

        /**
         * Generates common login module options.
         *
         * @param deployment
         * @return
         */
        private Map<String, String> getLoginModuleOptions() {
            final Map<String, String> options = new HashMap<String, String>();
            options.put("dsJndiName", DS_JNDI);
            options.put("principalsQuery", "select " + COLUMN_PASSWORD + " from " + TABLE_NAME_USERS + " where " + COLUMN_USER + "=?");
            options.put("rolesQuery", "select " + COLUMN_ROLE + ", " + COLUMN_ROLE_GROUP + " from " + TABLE_NAME_ROLES + " where "
                    + COLUMN_USER + "=?");
            return options;
        }
    }

}
