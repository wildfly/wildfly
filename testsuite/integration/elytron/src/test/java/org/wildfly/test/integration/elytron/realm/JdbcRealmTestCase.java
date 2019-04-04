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

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.security.Provider;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.Base64.Encoder;

import org.h2.tools.Server;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.security.common.AbstractDataSourceServerSetupTask;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.DataSource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.WildFlyElytronProvider;
import org.wildfly.security.password.PasswordFactory;
import org.wildfly.security.password.interfaces.BCryptPassword;
import org.wildfly.security.password.spec.EncryptablePasswordSpec;
import org.wildfly.security.password.spec.IteratedSaltedPasswordAlgorithmSpec;
import org.wildfly.security.permission.ElytronPermission;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.ConstantRealmMapper;
import org.wildfly.test.security.common.elytron.JdbcSecurityRealm;
import org.wildfly.test.security.common.elytron.MappedRegexRealmMapper;
import org.wildfly.test.security.common.elytron.RegexPrincipalTransformer;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain;
import org.wildfly.test.undertow.common.UndertowApplicationSecurityDomain;

/**
 * A test case to test the {@link JdbcSecurityRealm} within the Elytron subsystem.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({JdbcRealmTestCase.DataSourcesSetup.class,
              JdbcRealmTestCase.DBSetup.class,
              JdbcRealmTestCase.ServerSetup.class})
public class JdbcRealmTestCase {

    private static final String DEPLOYMENT = "JdbcRealmDeployment";

    private static final Provider PROVIDER = new WildFlyElytronProvider();

    private static final String DATASOURCE_NAME = "JdbcRealmTest";

    @Deployment(name = DEPLOYMENT)
    public static WebArchive deployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
        war.addClasses(JdbcTestServlet.class);
        war.addAsWebInfResource(JdbcRealmTestCase.class.getPackage(), "jdbc-realm-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(DEPLOYMENT), "jboss-web.xml")
        .addAsManifestResource(createPermissionsXmlAsset(
                new ElytronPermission("getSecurityDomain")),
                "permissions.xml");
        return war;
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testClearPassword_Success(@ArquillianResource URL webAppURL) throws Exception {
        String result = Utils.makeCallWithBasicAuthn(convert(webAppURL, "userOne%40wildfly.org", "Red", "Green"), "userOne@Clear", "passwordOne", SC_OK);

        assertEquals(JdbcTestServlet.RESPONSE_BODY, result);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testBCRYPTPassword_Success(@ArquillianResource URL webAppURL) throws Exception {
        String result = Utils.makeCallWithBasicAuthn(convert(webAppURL, "userTwo%40wildfly.org", "Black", "Blue"), "userTwo@BCRYPT", "passwordTwo", SC_OK);

        assertEquals(JdbcTestServlet.RESPONSE_BODY, result);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testCombinedClearPassword_Success(@ArquillianResource URL webAppURL) throws Exception {
        String result = Utils.makeCallWithBasicAuthn(convert(webAppURL, "userThree%40wildfly.org", "Yellow", "Orange"), "userThree@Combined", "passwordThree", SC_OK);

        assertEquals(JdbcTestServlet.RESPONSE_BODY, result);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testCombinedBCRYPTPassword_Success(@ArquillianResource URL webAppURL) throws Exception {
        String result = Utils.makeCallWithBasicAuthn(convert(webAppURL, "userFour%40wildfly.org", "White", "Purple"), "userFour@Combined", "passwordFour", SC_OK);

        assertEquals(JdbcTestServlet.RESPONSE_BODY, result);
    }

    private URL convert(URL original, final String email, final String... colours) throws Exception {
        StringBuilder sb = new StringBuilder(original.toExternalForm())
                .append(JdbcTestServlet.SERVLET_PATH.substring(1))
                .append('?')
                .append("email=").append(email);
        for (String colour : colours) {
            sb.append("&favourite-colours=").append(colour);
        }

        return new URL(sb.toString());
    }

    static class ServerSetup extends AbstractElytronSetupTask {

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            ConfigurableElement[] elements = new ConfigurableElement[10];

            // Clear Realm
            elements[0] = JdbcSecurityRealm.builder("clear_realm")
                    .withPrincipalQuery(DATASOURCE_NAME, "select password, email from clear_identities where name = ?")
                        .withPasswordMapper("clear-password-mapper", null, 1, -1, -1)
                        .withAttributeMapper("email", 2)
                        .build()
                    .withPrincipalQuery(DATASOURCE_NAME, "select colour from colours where name = ?")
                        .withAttributeMapper("favourite-colours", 1)
                        .build()
                    .build();
            // Clear Realm Constant Realm Mapper
            elements[1] = ConstantRealmMapper.newInstance("clear_realm_mapper", "clear_realm");
            // BCRYPT Realm
            elements[2] = JdbcSecurityRealm.builder("bcrypt_realm")
                .withPrincipalQuery(DATASOURCE_NAME, "select hash, salt, iteration_count, email from bcrypt_identities where name = ?")
                    .withPasswordMapper("bcrypt-mapper", null, 1, 2, 3)
                    .withAttributeMapper("email", 4)
                    .build()
                .withPrincipalQuery(DATASOURCE_NAME, "select colour from colours where name = ?")
                    .withAttributeMapper("favourite-colours", 1)
                    .build()
                .build();
            // BCRYPT Realm Mapper
            elements[3] = ConstantRealmMapper.newInstance("bcrypt_realm_mapper", "bcrypt_realm");
            // Combined Realm
            elements[4] = JdbcSecurityRealm.builder("combined_realm")
                .withPrincipalQuery(DATASOURCE_NAME, "select password, email from clear_identities where name = ?")
                    .withPasswordMapper("clear-password-mapper", null, 1, -1, -1)
                    .withAttributeMapper("email", 2)
                    .build()
                .withPrincipalQuery(DATASOURCE_NAME, "select hash, salt, iteration_count, email from bcrypt_identities where name = ?")
                    .withPasswordMapper("bcrypt-mapper", null, 1, 2, 3)
                    .withAttributeMapper("email", 4)
                    .build()
                .withPrincipalQuery(DATASOURCE_NAME, "select colour from colours where name = ?")
                    .withAttributeMapper("favourite-colours", 1)
                    .build()
                .build();
            // Combined Realm Mapper
            elements[5] = ConstantRealmMapper.newInstance("combined_realm_mapper", "combined_realm");
            // RegEx RealmMapper
            elements[6] = MappedRegexRealmMapper.builder("regex-mapper")
                    .withPattern(".+?@(.+)") // Reluctantly match all characters up to and including the first '@', all remaining characters into a single capturing group.
                    .withRealmMapping("Clear", "clear_realm")
                    .withRealmMapping("BCRYPT", "bcrypt_realm")
                    .withRealmMapping("Combined", "combined_realm")
                    .build();
            // Name Rewriter
            elements[7] = RegexPrincipalTransformer.builder("realm-stripper")
                    .withPattern("@.+")
                    .withReplacement("")
                    .build();
            // Security Domain
            elements[8] = SimpleSecurityDomain.builder()
                    .withName("JdbcTestDomain")
                    .withPostRealmPrincipalTransformer("realm-stripper")
                    .withRealmMapper("regex-mapper")
                    .withPermissionMapper("default-permission-mapper")
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder().withRealm("clear_realm").build(),
                            SimpleSecurityDomain.SecurityDomainRealm.builder().withRealm("bcrypt_realm").build(),
                            SimpleSecurityDomain.SecurityDomainRealm.builder().withRealm("combined_realm").build())
                    .build();

            // Undertow Application Security Domain
            elements[9] = UndertowApplicationSecurityDomain.builder()
                    .withName(DEPLOYMENT)
                    .withSecurityDomain("JdbcTestDomain")
                    .build();

            return elements;
        }

    }

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

            final Connection conn = DriverManager.getConnection(dbUrl, "sa", "sa");

            executeUpdate(conn, "create table clear_identities (name VARCHAR PRIMARY KEY, password VARCHAR, email VARCHAR)");
            executeUpdate(conn, "create table bcrypt_identities (name VARCHAR PRIMARY KEY,  hash VARCHAR, salt VARCHAR, iteration_count INT, email VARCHAR)");
            executeUpdate(conn, "create table colours (name VARCHAR, colour VARCHAR)");

            addClearUser(conn, "userOne", "passwordOne", "userOne@wildfly.org", "Red", "Green");
            addBCryptUser(conn, "userTwo", "passwordTwo", "userTwo@wildfly.org", "Black", "Blue");
            addClearUser(conn, "userThree", "passwordThree", "userThree@wildfly.org", "Yellow", "Orange");
            addBCryptUser(conn, "userFour", "passwordFour", "userFour@wildfly.org", "White", "Purple");

            conn.close();
        }

        private void addClearUser(final Connection conn, final String username, final String password, final String eMail, final String... colours) throws SQLException {
            executeUpdate(conn, String.format("insert into clear_identities VALUES ('%s', '%s', '%s')", username, password, eMail));
            addFavouriteColours(conn, username, colours);
        }

        private void addBCryptUser(final Connection conn, final String username, final String password, final String eMail, final String... colours) throws Exception {
            int iterationCount = 10;

            byte[] salt = new byte[BCryptPassword.BCRYPT_SALT_SIZE];
            SecureRandom random = new SecureRandom();
            random.nextBytes(salt);

            PasswordFactory passwordFactory = PasswordFactory.getInstance(BCryptPassword.ALGORITHM_BCRYPT, PROVIDER);
            IteratedSaltedPasswordAlgorithmSpec iteratedAlgorithmSpec = new IteratedSaltedPasswordAlgorithmSpec(iterationCount, salt);
            EncryptablePasswordSpec encryptableSpec = new EncryptablePasswordSpec(password.toCharArray(), iteratedAlgorithmSpec);

            BCryptPassword original = (BCryptPassword) passwordFactory.generatePassword(encryptableSpec);

            byte[] hash = original.getHash();

            Encoder encoder = Base64.getEncoder();
            String encodedHash = encoder.encodeToString(hash);
            String encodedSalt = encoder.encodeToString(salt);

            executeUpdate(conn, String.format("insert into bcrypt_identities VALUES ('%s', '%s', '%s', %d, '%s')", username, encodedHash, encodedSalt, iterationCount, eMail));
            addFavouriteColours(conn, username, colours);
        }

        private void addFavouriteColours(final Connection conn, final String username, final String... colours) throws SQLException {
            for (String colour : colours) {
                executeUpdate(conn, String.format("insert into colours VALUES ('%s', '%s')", username, colour));
            }
        }

        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            server.shutdown();
            server = null;
        }

        private void executeUpdate(Connection connection, String query) throws SQLException {
            final Statement statement = connection.createStatement();
            statement.executeUpdate(query);
            statement.close();
        }
    }



}
