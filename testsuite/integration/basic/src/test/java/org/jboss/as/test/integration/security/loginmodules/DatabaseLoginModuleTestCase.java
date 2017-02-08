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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.h2.tools.Server;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.security.common.AbstractDataSourceServerSetupTask;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.Coding;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.DataSource;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
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
 * Tests for {@link org.jboss.security.auth.spi.DatabaseServerLoginModule}. It uses embedded H2 database as a datastore for
 * users and roles.
 *
 * @author Jan Lanik
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup({DatabaseLoginModuleTestCase.DBSetup.class, //
        DatabaseLoginModuleTestCase.DataSourcesSetup.class, //
        DatabaseLoginModuleTestCase.SecurityDomainsSetup.class //
})
@RunAsClient
@Category(CommonCriteria.class)
public class DatabaseLoginModuleTestCase {

    private static Logger LOGGER = Logger.getLogger(DatabaseLoginModuleTestCase.class);

    private static final String MARCUS = "marcus";
    private static final String ANIL = "anil";

    private static final String DATASOURCE_NAME = "DBLMTest";

    private static final String DEP1 = "DEP1"; //"DatabaseLogin-defaultSetting";
    private static final String DEP2 = "DEP2"; //"DatabaseLogin-hashMD5";
    private static final String DEP3 = "DEP3"; //"DatabaseLogin-hashMD5-base64";
    private static final String DEP4 = "DEP4"; //"DatabaseLogin-hashMD5-hex";

    private static final String MD5 = "MD5";

    /**
     * Creates WAR for test login module with default settings.
     *
     * @return
     */
    @Deployment(name = DEP1)
    public static WebArchive appDeployment1() {
        return createWar(DEP1);
    }

    /**
     * Creates WAR for test login module with MD5 hashing enabled.
     *
     * @return
     */
    @Deployment(name = DEP2)
    public static WebArchive appDeployment2() {
        return createWar(DEP2);
    }

    /**
     * Creates WAR for test login module with MD5 hashing enabled - Base64 coding used.
     *
     * @return
     */
    @Deployment(name = DEP3)
    public static WebArchive appDeployment3() {
        return createWar(DEP3);
    }

    /**
     * Creates WAR for test login module with MD5 hashing enabled - HEX coding used.
     *
     * @return
     */
    @Deployment(name = DEP4)
    public static WebArchive appDeployment4() {
        return createWar(DEP4);
    }

    /**
     * Test default login module settings.
     */
    @OperateOnDeployment(DEP1)
    @Test
    public void testDefault(@ArquillianResource URL url) throws Exception {
        testAccess(url);
    }

    /**
     * Test login module setting with MD5 hashing enabled.
     */
    @OperateOnDeployment(DEP2)
    @Test
    public void testHashed(@ArquillianResource URL url) throws Exception {
        testAccess(url);
    }

    /**
     * Test login module setting with MD5 hashing enabled - Base64 coding used.
     */
    @OperateOnDeployment(DEP3)
    @Test
    public void testHashedBase64(@ArquillianResource URL url) throws Exception {
        testAccess(url);
    }

    /**
     * Test login module setting with MD5 hashing enabled - HEX coding used.
     */
    @OperateOnDeployment(DEP4)
    @Test
    public void testHashedHex(@ArquillianResource URL url) throws Exception {
        testAccess(url);
    }

    // Private methods -------------------------------------------------------

    /**
     * Tests access to a protected servlet.
     *
     * @param url
     * @throws MalformedURLException
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     */
    private void testAccess(URL url) throws IOException, URISyntaxException {
        final URL servletUrl = new URL(url.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));
        //successful authentication and authorization
        assertEquals("Response body is not correct.", SimpleSecuredServlet.RESPONSE_BODY,
                Utils.makeCallWithBasicAuthn(servletUrl, ANIL, ANIL, 200));
        //successful authentication and unsuccessful authorization
        Utils.makeCallWithBasicAuthn(servletUrl, MARCUS, MARCUS, 403);
        //unsuccessful authentication
        Utils.makeCallWithBasicAuthn(servletUrl, ANIL, MARCUS, 401);
        Utils.makeCallWithBasicAuthn(servletUrl, ANIL, Utils.hash(ANIL, MD5, Coding.BASE_64), 401);
        Utils.makeCallWithBasicAuthn(servletUrl, ANIL, Utils.hash(ANIL, MD5, Coding.HEX), 401);
    }

    /**
     * Creates {@link WebArchive} (WAR) for given deployment name.
     *
     * @param deployment
     * @return
     */
    private static WebArchive createWar(final String deployment) {

        final WebArchive war = ShrinkWrap.create(WebArchive.class, deployment + ".war");
        war.addClasses(SimpleSecuredServlet.class, SimpleServlet.class);
        war.addAsWebInfResource(DatabaseLoginModuleTestCase.class.getPackage(), "web-basic-authn.xml", "web.xml");
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
            final SecurityModule.Builder loginModuleBuilder = new SecurityModule.Builder().name("Database").options(
                    getLoginModuleOptions(DEP1));
            final SecurityDomain sd1 = new SecurityDomain.Builder().name(DEP1).loginModules(loginModuleBuilder.build()).build();
            loginModuleBuilder.options(getLoginModuleOptions(DEP2)).putOption("hashAlgorithm", MD5);
            final SecurityDomain sd2 = new SecurityDomain.Builder().name(DEP2).loginModules(loginModuleBuilder.build()).build();
            loginModuleBuilder.options(getLoginModuleOptions(DEP3)).putOption("hashAlgorithm", MD5)
                    .putOption("hashEncoding", "base64");
            final SecurityDomain sd3 = new SecurityDomain.Builder().name(DEP3).loginModules(loginModuleBuilder.build()).build();
            loginModuleBuilder.options(getLoginModuleOptions(DEP4)).putOption("hashAlgorithm", MD5)
                    .putOption("hashEncoding", "hex");
            final SecurityDomain sd4 = new SecurityDomain.Builder().name(DEP4).loginModules(loginModuleBuilder.build()).build();
            return new SecurityDomain[]{sd1, sd2, sd3, sd4};
        }

        /**
         * Generates common login module options.
         *
         * @param deployment
         * @return
         */
        private Map<String, String> getLoginModuleOptions(String deployment) {
            final Map<String, String> options = new HashMap<String, String>();
            options.put("dsJndiName", "java:jboss/datasources/" + DATASOURCE_NAME);
            options.put("principalsQuery", "select Password from Principals" + deployment + " where PrincipalID=?");
            options.put("rolesQuery", "select Role, RoleGroup from Roles where PrincipalID=?");
            return options;
        }
    }

    /**
     * Datasource setup task for H2 DB.
     */
    static class DataSourcesSetup extends AbstractDataSourceServerSetupTask {

        @Override
        protected DataSource[] getDataSourceConfigurations(ManagementClient managementClient, String containerId) {
            return new DataSource[]{new DataSource.Builder()
                    .name(DATASOURCE_NAME)
                    .connectionUrl(
                            "jdbc:h2:tcp://" + Utils.getSecondaryTestAddress(managementClient) + "/mem:" + DATASOURCE_NAME)
                    .driver("h2").username("sa").password("sa").build()};
        }
    }

    /**
     * H2 DB configuration setup task.
     */
    static class DBSetup implements ServerSetupTask {

        private Server server;

        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            server = Server.createTcpServer("-tcpAllowOthers").start();
            final String dbUrl = "jdbc:h2:mem:" + DATASOURCE_NAME + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
            LOGGER.trace("Creating database " + dbUrl);

            final Connection conn = DriverManager.getConnection(dbUrl, "sa", "sa");
            executeUpdate(conn, "CREATE TABLE Roles(PrincipalID Varchar(50), Role Varchar(50), RoleGroup Varchar(50))");
            executeUpdate(conn, "INSERT INTO Roles VALUES ('anil','" + SimpleSecuredServlet.ALLOWED_ROLE + "','Roles')");
            executeUpdate(conn, "INSERT INTO Roles VALUES ('marcus','superuser','Roles')");
            createPrincipalsTab(conn, DEP1, null);
            createPrincipalsTab(conn, DEP2, Coding.BASE_64);
            createPrincipalsTab(conn, DEP3, Coding.BASE_64);
            createPrincipalsTab(conn, DEP4, Coding.HEX);
            conn.close();
        }

        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            server.shutdown();
            server = null;
        }

        private void createPrincipalsTab(Connection conn, String dep, Coding coding) throws SQLException {
            executeUpdate(conn, "CREATE TABLE Principals" + dep + "(PrincipalID Varchar(50), Password Varchar(50))");
            executeUpdate(conn, "INSERT INTO Principals" + dep + " VALUES ('anil','" + Utils.hashMD5(ANIL, coding) + "')");
            executeUpdate(conn, "INSERT INTO Principals" + dep + " VALUES ('marcus','" + Utils.hashMD5(MARCUS, coding) + "')");
        }

        private void executeUpdate(Connection connection, String query) throws SQLException {
            final Statement statement = connection.createStatement();
            final int updateResult = statement.executeUpdate(query);
            LOGGER.trace("Result: " + updateResult + ".  SQL statement: " + query);
            statement.close();
        }
    }

}
