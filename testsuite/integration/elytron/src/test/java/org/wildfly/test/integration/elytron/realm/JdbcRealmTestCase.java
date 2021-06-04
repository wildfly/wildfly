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
import java.nio.charset.Charset;
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
import org.wildfly.common.iteration.ByteIterator;
import org.wildfly.security.WildFlyElytronProvider;
import org.wildfly.security.password.PasswordFactory;
import org.wildfly.security.password.interfaces.BCryptPassword;
import org.wildfly.security.password.interfaces.BSDUnixDESCryptPassword;
import org.wildfly.security.password.spec.EncryptablePasswordSpec;
import org.wildfly.security.password.spec.IteratedSaltedPasswordAlgorithmSpec;
import org.wildfly.security.password.util.ModularCrypt;
import org.wildfly.security.permission.ElytronPermission;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.ConstantRealmMapper;
import org.wildfly.test.security.common.elytron.JdbcSecurityRealm;
import org.wildfly.test.security.common.elytron.JdbcSecurityRealm.Encoding;
import org.wildfly.test.security.common.elytron.MappedRegexRealmMapper;
import org.wildfly.test.security.common.elytron.RegexPrincipalTransformer;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain;
import org.wildfly.test.undertow.common.UndertowApplicationSecurityDomain;
import com.nimbusds.jose.util.StandardCharset;

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

    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testBCRYPTHexPassword_Success(@ArquillianResource URL webAppURL) throws Exception {
        String result = Utils.makeCallWithBasicAuthn(convert(webAppURL, "userFive%40wildfly.org", "Violet", "Amber"), "userFive@BCRYPT_HEX", "passwordFive", SC_OK);

        assertEquals(JdbcTestServlet.RESPONSE_BODY, result);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testCombinedBCRYPTHexPassword_Success(@ArquillianResource URL webAppURL) throws Exception {
        String result = Utils.makeCallWithBasicAuthn(convert(webAppURL, "userSix%40wildfly.org", "Pink", "Gold"), "userSix@Combined", "passwordSix", SC_OK);

        assertEquals(JdbcTestServlet.RESPONSE_BODY, result);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testModularCryptPassword_Success(@ArquillianResource URL webAppURL) throws Exception {
        String result = Utils.makeCallWithBasicAuthn(convert(webAppURL, "userSeven%40wildfly.org", "Grey", "Indigo"), "userSeven@MODULAR_CRYPT", "passwordSeven", SC_OK);

        assertEquals(JdbcTestServlet.RESPONSE_BODY, result);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testCombinedModularCryptPassword_Success(@ArquillianResource URL webAppURL) throws Exception {
        String result = Utils.makeCallWithBasicAuthn(convert(webAppURL, "userEight%40wildfly.org", "Lilac", "Bergundy"), "userEight@Combined", "passwordEight", SC_OK);

        assertEquals(JdbcTestServlet.RESPONSE_BODY, result);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testBCRYPTCharsetPassword_Success(@ArquillianResource URL webAppURL) throws Exception {
        String result = Utils.makeCallWithBasicAuthn(convert(webAppURL, "userNine%40wildfly.org", "Red"), "userNine@BCRYPT_CHARSET", "password密码", SC_OK);
        assertEquals(JdbcTestServlet.RESPONSE_BODY, result);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testBCRYPTCharsetHexEncodedPassword_Success(@ArquillianResource URL webAppURL) throws Exception {
        String result = Utils.makeCallWithBasicAuthn(convert(webAppURL, "userTen%40wildfly.org", "Red"), "userTen@BCRYPT_CHARSET_HEX", "password密码", SC_OK);
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
            ConfigurableElement[] elements = new ConfigurableElement[18];

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
            // BCRYPT Hex Realm
            elements[4] = JdbcSecurityRealm.builder("bcrypt_hex_realm")
                    .withPrincipalQuery(DATASOURCE_NAME, "select hash, salt, iteration_count, email from bcrypt_hex_identities where name = ?")
                        .withPasswordMapper("bcrypt-mapper", null, 1, Encoding.HEX, 2, Encoding.HEX, 3)
                        .withAttributeMapper("email", 4)
                        .build()
                    .withPrincipalQuery(DATASOURCE_NAME, "select colour from colours where name = ?")
                        .withAttributeMapper("favourite-colours", 1)
                        .build()
                    .build();
            // BCRYPT Hex Mapper
            elements[5] = ConstantRealmMapper.newInstance("bcrypt_hex_realm_mapper", "bcrypt_hex_realm");
            // BCRYPT Charset Realm
            elements[6] = JdbcSecurityRealm.builder("bcrypt_charset_realm")
                    .withHashCharset("GB2312")
                    .withPrincipalQuery(DATASOURCE_NAME, "select hash, salt, iteration_count, email from bcrypt_identities_charset where name = ?")
                    .withPasswordMapper("bcrypt-mapper", null, 1, 2, 3)
                    .withAttributeMapper("email", 4)
                    .build()
                    .withPrincipalQuery(DATASOURCE_NAME, "select colour from colours where name = ?")
                    .withAttributeMapper("favourite-colours", 1)
                    .build()
                    .build();
            // BCRYPT Charset Mapper
            elements[7] = ConstantRealmMapper.newInstance("bcrypt_charset_realm_mapper", "bcrypt_charset_realm");
            // BCRYPT Charset Hex Realm
            elements[8] = JdbcSecurityRealm.builder("bcrypt_charset_hex_realm")
                    .withHashCharset("GB2312")
                    .withPrincipalQuery(DATASOURCE_NAME, "select hash, salt, iteration_count, email from bcrypt_hex_identities_charset where name = ?")
                    .withPasswordMapper("bcrypt-mapper", null, 1, Encoding.HEX, 2, Encoding.HEX, 3)
                    .withAttributeMapper("email", 4)
                    .build()
                    .withPrincipalQuery(DATASOURCE_NAME, "select colour from colours where name = ?")
                    .withAttributeMapper("favourite-colours", 1)
                    .build()
                    .build();
            // BCRYPT Charset Hex Mapper
            elements[9] = ConstantRealmMapper.newInstance("bcrypt_charset_hex_realm_mapper", "bcrypt_charset_hex_realm");
            // Modular Crypt Realm
            elements[10] = JdbcSecurityRealm.builder("modular_crypt_realm")
                    .withPrincipalQuery(DATASOURCE_NAME, "select password, email from modular_crypt_identities where name = ?")
                        .withPasswordMapper("modular-crypt-mapper", null, 1, -1, -1)
                        .withAttributeMapper("email", 2)
                        .build()
                    .withPrincipalQuery(DATASOURCE_NAME, "select colour from colours where name = ?")
                        .withAttributeMapper("favourite-colours", 1)
                        .build()
                    .build();
            // Modular Crypt Mapper
            elements[11] = ConstantRealmMapper.newInstance("modular_crypt_realm_mapper", "modular_crypt_realm");
            // Combined Realm
            elements[12] = JdbcSecurityRealm.builder("combined_realm")
                .withPrincipalQuery(DATASOURCE_NAME, "select password, email from clear_identities where name = ?")
                    .withPasswordMapper("clear-password-mapper", null, 1, -1, -1)
                    .withAttributeMapper("email", 2)
                    .build()
                .withPrincipalQuery(DATASOURCE_NAME, "select hash, salt, iteration_count, email from bcrypt_identities where name = ?")
                    .withPasswordMapper("bcrypt-mapper", null, 1, 2, 3)
                    .withAttributeMapper("email", 4)
                    .build()
                .withPrincipalQuery(DATASOURCE_NAME, "select hash, salt, iteration_count, email from bcrypt_hex_identities where name = ?")
                    .withPasswordMapper("bcrypt-mapper", null, 1, Encoding.HEX, 2, Encoding.HEX, 3)
                    .withAttributeMapper("email", 4)
                    .build()
                .withPrincipalQuery(DATASOURCE_NAME, "select password, email from modular_crypt_identities where name = ?")
                    .withPasswordMapper("modular-crypt-mapper", null, 1, -1, -1)
                    .withAttributeMapper("email", 2)
                    .build()
                .withPrincipalQuery(DATASOURCE_NAME, "select colour from colours where name = ?")
                    .withAttributeMapper("favourite-colours", 1)
                    .build()
                .build();
            // Combined Realm Mapper
            elements[13] = ConstantRealmMapper.newInstance("combined_realm_mapper", "combined_realm");
            // RegEx RealmMapper
            elements[14] = MappedRegexRealmMapper.builder("regex-mapper")
                    .withPattern(".+?@(.+)") // Reluctantly match all characters up to and including the first '@', all remaining characters into a single capturing group.
                    .withRealmMapping("Clear", "clear_realm")
                    .withRealmMapping("BCRYPT", "bcrypt_realm")
                    .withRealmMapping("BCRYPT_HEX", "bcrypt_hex_realm")
                    .withRealmMapping("MODULAR_CRYPT", "modular_crypt_realm")
                    .withRealmMapping("Combined", "combined_realm")
                    .withRealmMapping("BCRYPT_CHARSET", "bcrypt_charset_realm")
                    .withRealmMapping("BCRYPT_CHARSET_HEX", "bcrypt_charset_hex_realm")
                    .build();
            // Name Rewriter
            elements[15] = RegexPrincipalTransformer.builder("realm-stripper")
                    .withPattern("@.+")
                    .withReplacement("")
                    .build();

            // Security Domain
            elements[16] = SimpleSecurityDomain.builder()
                    .withName("JdbcTestDomain")
                    .withPostRealmPrincipalTransformer("realm-stripper")
                    .withRealmMapper("regex-mapper")
                    .withPermissionMapper("default-permission-mapper")
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder().withRealm("clear_realm").build(),
                            SimpleSecurityDomain.SecurityDomainRealm.builder().withRealm("bcrypt_realm").build(),
                            SimpleSecurityDomain.SecurityDomainRealm.builder().withRealm("bcrypt_hex_realm").build(),
                            SimpleSecurityDomain.SecurityDomainRealm.builder().withRealm("modular_crypt_realm").build(),
                            SimpleSecurityDomain.SecurityDomainRealm.builder().withRealm("combined_realm").build(),
                            SimpleSecurityDomain.SecurityDomainRealm.builder().withRealm("bcrypt_charset_realm").build(),
                            SimpleSecurityDomain.SecurityDomainRealm.builder().withRealm("bcrypt_charset_hex_realm").build())
                    .build();

            // Undertow Application Security Domain
            elements[17] = UndertowApplicationSecurityDomain.builder()
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
            executeUpdate(conn, "create table bcrypt_hex_identities (name VARCHAR PRIMARY KEY,  hash VARCHAR, salt VARCHAR, iteration_count INT, email VARCHAR)");
            executeUpdate(conn, "create table colours (name VARCHAR, colour VARCHAR)");
            executeUpdate(conn, "create table modular_crypt_identities (name VARCHAR PRIMARY KEY, password VARCHAR, email VARCHAR)");
            executeUpdate(conn, "create table bcrypt_identities_charset (name VARCHAR PRIMARY KEY, hash VARCHAR, salt VARCHAR, iteration_count INT, email VARCHAR)");
            executeUpdate(conn, "create table bcrypt_hex_identities_charset (name VARCHAR PRIMARY KEY, hash VARCHAR, salt VARCHAR, iteration_count INT, email VARCHAR)");


            addClearUser(conn, "userOne", "passwordOne", "userOne@wildfly.org", "Red", "Green");
            addBCryptUser(conn, "userTwo", "passwordTwo", "userTwo@wildfly.org", "bcrypt_identities", false, "Black", "Blue");
            addClearUser(conn, "userThree", "passwordThree", "userThree@wildfly.org", "Yellow", "Orange");
            addBCryptUser(conn, "userFour", "passwordFour", "userFour@wildfly.org", "bcrypt_identities", false, "White", "Purple");
            addBCryptUser(conn, "userFive", "passwordFive", "userFive@wildfly.org", "bcrypt_hex_identities", true, "Violet", "Amber");
            addBCryptUser(conn, "userSix", "passwordSix", "userSix@wildfly.org", "bcrypt_hex_identities", true, "Pink", "Gold");
            addModularCryptUser(conn, "userSeven", "passwordSeven", "userSeven@wildfly.org", "Grey", "Indigo");
            addModularCryptUser(conn, "userEight", "passwordEight", "userEight@wildfly.org", "Lilac", "Bergundy");
            addBCryptUser(conn, "userNine", "password密码", "userNine@wildfly.org", "bcrypt_identities_charset", false, Charset.forName("GB2312"), "Red");
            addBCryptUser(conn, "userTen", "password密码", "userTen@wildfly.org", "bcrypt_hex_identities_charset", true, Charset.forName("GB2312"), "Red");
            conn.close();
        }

        private void addClearUser(final Connection conn, final String username, final String password, final String eMail, final String... colours) throws SQLException {
            executeUpdate(conn, String.format("insert into clear_identities VALUES ('%s', '%s', '%s')", username, password, eMail));
            addFavouriteColours(conn, username, colours);
        }

        private void addBCryptUser(final Connection conn, final String username, final String password, final String eMail, String tableName,  final boolean hexEncoded,final String... colours) throws Exception {
            addBCryptUser(conn, username, password, eMail, tableName,  hexEncoded, StandardCharset.UTF_8, colours);
        }

        private void addBCryptUser(final Connection conn, final String username, final String password, final String eMail, String tableName, final boolean hexEncoded, final Charset hashCharset, final String... colours) throws Exception {
            int iterationCount = 10;

            byte[] salt = new byte[BCryptPassword.BCRYPT_SALT_SIZE];
            SecureRandom random = new SecureRandom();
            random.nextBytes(salt);

            PasswordFactory passwordFactory = PasswordFactory.getInstance(BCryptPassword.ALGORITHM_BCRYPT, PROVIDER);
            IteratedSaltedPasswordAlgorithmSpec iteratedAlgorithmSpec = new IteratedSaltedPasswordAlgorithmSpec(iterationCount, salt);
            EncryptablePasswordSpec encryptableSpec = new EncryptablePasswordSpec(password.toCharArray(), iteratedAlgorithmSpec, hashCharset);

            BCryptPassword original = (BCryptPassword) passwordFactory.generatePassword(encryptableSpec);

            byte[] hash = original.getHash();

            Encoder encoder = Base64.getEncoder();

            final String encodedHash;
            final String encodedSalt;

            if (hexEncoded) {
                encodedHash = ByteIterator.ofBytes(hash).hexEncode().drainToString();
                encodedSalt = ByteIterator.ofBytes(salt).hexEncode().drainToString();
            } else {
                encodedHash = encoder.encodeToString(hash);
                encodedSalt = encoder.encodeToString(salt);
            }

            executeUpdate(conn, String.format("insert into %s VALUES ('%s', '%s', '%s', %d, '%s')", tableName, username, encodedHash, encodedSalt, iterationCount, eMail));
            addFavouriteColours(conn, username, colours);
        }

        private void addModularCryptUser(final Connection conn, final String username, final String password, final String eMail, final String... colours) throws Exception {
            PasswordFactory passwordFactory = PasswordFactory.getInstance(BSDUnixDESCryptPassword.ALGORITHM_BSD_CRYPT_DES, PROVIDER);

            int iterationCount = BSDUnixDESCryptPassword.DEFAULT_ITERATION_COUNT;

            byte[] salt = new byte[BSDUnixDESCryptPassword.BSD_CRYPT_DES_SALT_SIZE];
            SecureRandom random = new SecureRandom();
            random.nextBytes(salt);

            IteratedSaltedPasswordAlgorithmSpec iteratedAlgorithmSpec = new IteratedSaltedPasswordAlgorithmSpec(iterationCount, salt);
            EncryptablePasswordSpec encryptableSpec = new EncryptablePasswordSpec(password.toCharArray(), iteratedAlgorithmSpec);

            BSDUnixDESCryptPassword original = (BSDUnixDESCryptPassword) passwordFactory.generatePassword(encryptableSpec);

            String encoded = ModularCrypt.encodeAsString(original);

            executeUpdate(conn, String.format("insert into modular_crypt_identities VALUES ('%s', '%s', '%s')", username, encoded, eMail));
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
