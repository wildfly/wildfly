/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.security.realms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import javax.naming.directory.DirContext;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.sasl.RealmCallback;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.factory.DirectoryServiceFactory;
import org.apache.directory.server.core.factory.PartitionFactory;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.jboss.as.domain.management.AuthMechanism;
import org.jboss.as.domain.management.AuthorizingCallbackHandler;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.domain.management.connections.ConnectionManager;
import org.jboss.as.domain.management.connections.ldap.LdapConnectionManagerService;
import org.jboss.as.domain.management.security.operations.OutboundConnectionAddBuilder;
import org.jboss.as.domain.management.security.operations.SecurityRealmAddBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.sasl.callback.VerifyPasswordCallback;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A test case to test authentication against an LDAP server.
 *
 * This initial test starts it's own LDAP server to authenticate against, this will be evolved further so the LDAP server can be
 * started once for a suite of test cases.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class LdapAuthenticationTestCase extends SecurityRealmTestBase {

    private static final String HOST_NAME = "localhost";
    private static int LDAP_PORT = 11389;
    private static final String DIRECTORY_NAME = "Test Service";
    private static final String CONNECTION_NAME = "TestConnection";

    private static final String ADVANCED_REALM = "AdvancedRealm";

    private static final String BASE_DN = "dc=simple,dc=wildfly,dc=org";
    private static final String ADVANCED_FILTER = "(&(uid={0})(departmentNumber=1))";
    private static final String USERNAME_FILTER = "uid";

    private static final String USER_ONE = "user_one";
    private static final String USER_ONE_PASSWORD = "one_password";
    private static final String USER_TWO = "user_two";
    private static final String USER_TWO_PASSWORD = "two_password";

    private static File workingDir;
    private static DirectoryService directoryService;
    private static LdapServer ldapServer;

    @BeforeClass
    public static void startLdapServer() throws Exception {
        createWorkingDir();
        DirectoryServiceFactory dsf = new DefaultDirectoryServiceFactory();
        dsf.init(DIRECTORY_NAME);
        directoryService = dsf.getDirectoryService();
        directoryService.getChangeLog().setEnabled(false);
        SchemaManager schemaManager = directoryService.getSchemaManager();

        PartitionFactory pf = dsf.getPartitionFactory();
        Partition p = pf.createPartition(schemaManager, "simple", "dc=simple,dc=wildfly,dc=org", 1000, workingDir);
        pf.addIndex(p, "uid", 5);
        pf.addIndex(p, "departmentNumber", 5);
        p.initialize();
        directoryService.addPartition(p);

        CoreSession adminSession = directoryService.getAdminSession();
        InputStream ldifInput = LdapAuthenticationTestCase.class.getResourceAsStream("simple-partition.ldif");
        LdifReader ldifReader = new LdifReader(ldifInput);
        for (LdifEntry ldifEntry : ldifReader) {
            adminSession.add(new DefaultEntry(schemaManager, ldifEntry.getEntry()));
        }
        ldifReader.close();
        ldifInput.close();

        ldapServer = new LdapServer();
        ldapServer.setServiceName("DefaultLDAP");
        Transport ldap = new TcpTransport( "0.0.0.0", LDAP_PORT, 3, 5 );
        ldapServer.addTransports(ldap);
        ldapServer.setDirectoryService(directoryService);
        ldapServer.start();

        System.out.println("Started");
    }

    private static void createWorkingDir() throws IOException {
        if (workingDir == null) {
            if (workingDir == null) {
                workingDir = new File(".");
                workingDir = new File(workingDir, "target");
                workingDir = new File(workingDir, "apacheds_working").getCanonicalFile();
                if (!workingDir.exists()) {
                    workingDir.mkdirs();
                }
            }
        }
        for (File current : workingDir.listFiles()) {
          current.delete();
        }
    }

    @AfterClass
    public static void stopLdapServer() throws Exception {
        if (ldapServer != null) {
            ldapServer.stop();
        }
        if (directoryService != null) {
            directoryService.shutdown();
        }
        workingDir = null;
    }

    @Test
    public void testConnection() throws Exception {
        ConnectionManager connectionManager = getConnectionManager(CONNECTION_NAME);
        assertNotNull("Connection Manager.", connectionManager);
        // Configured Credentials.
        DirContext connection = (DirContext) connectionManager.getConnection();
        assertNotNull("Connection with configured credentials.", connection);
        connection.close();
        // Supplied Credentials.
        connection = (DirContext) connectionManager.getConnection("uid=UserOne,dc=simple,dc=wildfly,dc=org","one_password");
        assertNotNull("Connection with configured credentials.", connection);
        connection.close();

        // Bad Supplied Credentials.
        try {
            connectionManager.getConnection("uid=UserOne,dc=simple,dc=wildfly,dc=org","bad_password");
            fail("Expected exception not thrown.");
        } catch (Exception ignored) {}
    }

    private ConnectionManager getConnectionManager(final String name) {
        return (ConnectionManager) getContainer().getService(LdapConnectionManagerService.ServiceUtil.createServiceName(name)).getValue();
    }

    @Test
    public void testSupportedMechanism() {
        Set<AuthMechanism> supportedMechs = securityRealm.getSupportedAuthenticationMechanisms();
        assertEquals("Number of mechanims", 1, supportedMechs.size());
        assertTrue("Supports Digest", supportedMechs.contains(AuthMechanism.PLAIN));
    }


    @Test
    public void testVerifyGoodPassword() throws Exception {
        AuthorizingCallbackHandler cbh = securityRealm.getAuthorizingCallbackHandler(AuthMechanism.PLAIN);

        NameCallback ncb = new NameCallback("Username", USER_ONE);
        RealmCallback rcb = new RealmCallback("Realm", TEST_REALM);
        VerifyPasswordCallback vpc = new VerifyPasswordCallback(USER_ONE_PASSWORD);

        cbh.handle(new Callback[] { ncb, rcb, vpc });

        assertTrue("Password Verified", vpc.isVerified());
    }

    @Test
    public void testVerifyGoodPassword_UserTwo() throws Exception {
        /*
         * Essentially a duplicate of the previous test but we want to be sure this works as we later
         * test that this user can be excluded using an advanced filter.
         */
        AuthorizingCallbackHandler cbh = securityRealm.getAuthorizingCallbackHandler(AuthMechanism.PLAIN);

        NameCallback ncb = new NameCallback("Username", USER_TWO);
        RealmCallback rcb = new RealmCallback("Realm", TEST_REALM);
        VerifyPasswordCallback vpc = new VerifyPasswordCallback(USER_TWO_PASSWORD);

        cbh.handle(new Callback[] { ncb, rcb, vpc });

        assertTrue("Password Verified", vpc.isVerified());
    }

    @Test
    public void testVerifyBadPassword() throws Exception {
        AuthorizingCallbackHandler cbh = securityRealm.getAuthorizingCallbackHandler(AuthMechanism.PLAIN);

        NameCallback ncb = new NameCallback("Username", USER_ONE);
        RealmCallback rcb = new RealmCallback("Realm", TEST_REALM);
        VerifyPasswordCallback vpc = new VerifyPasswordCallback(USER_TWO_PASSWORD);

        cbh.handle(new Callback[] { ncb, rcb, vpc });

        assertFalse("Password Not Verified", vpc.isVerified());
    }


    @Test
    public void testVerifyNonExistantUser() throws Exception {
        AuthorizingCallbackHandler cbh = securityRealm.getAuthorizingCallbackHandler(AuthMechanism.PLAIN);

        NameCallback ncb = new NameCallback("Username", "UserThree");
        RealmCallback rcb = new RealmCallback("Realm", TEST_REALM);
        VerifyPasswordCallback vpc = new VerifyPasswordCallback("three-password");

        try {
            cbh.handle(new Callback[] { ncb, rcb, vpc });
            fail("Expected exception not thrown");
        } catch (IOException e) {
        }
    }

    @Test
    public void testVerifyEmptyPassword() throws Exception {
        AuthorizingCallbackHandler cbh = securityRealm.getAuthorizingCallbackHandler(AuthMechanism.PLAIN);

        NameCallback ncb = new NameCallback("Username", USER_ONE);
        RealmCallback rcb = new RealmCallback("Realm", TEST_REALM);
        VerifyPasswordCallback vpc = new VerifyPasswordCallback("");

        try {
            cbh.handle(new Callback[] { ncb, rcb, vpc });
            fail("Expected exception not thrown");
        } catch (IOException e) {
        }
    }

    /*
     * Custom Realm, also filter by additional attribute.
     */

    private AuthorizingCallbackHandler getAdvancedCallbackHandler() {
        return ((SecurityRealm) getContainer().getService(SecurityRealm.ServiceUtil.createServiceName(ADVANCED_REALM))
                .getValue()).getAuthorizingCallbackHandler(AuthMechanism.PLAIN);
    }

    @Test
    public void testVerifyGoodPassword_Advanced() throws Exception {
        AuthorizingCallbackHandler cbh = getAdvancedCallbackHandler();

        NameCallback ncb = new NameCallback("Username", USER_ONE);
        RealmCallback rcb = new RealmCallback("Realm", TEST_REALM);
        VerifyPasswordCallback vpc = new VerifyPasswordCallback(USER_ONE_PASSWORD);

        cbh.handle(new Callback[] { ncb, rcb, vpc });

        assertTrue("Password Verified", vpc.isVerified());
    }

    @Test
    public void testVerifyBadPassword_Advanced() throws Exception {
        AuthorizingCallbackHandler cbh = getAdvancedCallbackHandler();

        NameCallback ncb = new NameCallback("Username", USER_ONE);
        RealmCallback rcb = new RealmCallback("Realm", TEST_REALM);
        VerifyPasswordCallback vpc = new VerifyPasswordCallback(USER_TWO_PASSWORD);

        cbh.handle(new Callback[] { ncb, rcb, vpc });

        assertFalse("Password Not Verified", vpc.isVerified());
    }

    @Test
    public void testVerifyFilteredOutUser() throws Exception {
        AuthorizingCallbackHandler cbh = getAdvancedCallbackHandler();

        NameCallback ncb = new NameCallback("Username", USER_TWO);
        RealmCallback rcb = new RealmCallback("Realm", TEST_REALM);
        VerifyPasswordCallback vpc = new VerifyPasswordCallback(USER_TWO_PASSWORD);

        try {
            cbh.handle(new Callback[] { ncb, rcb, vpc });
            fail("Expected exception not thrown");
        } catch (IOException e) {
        }
    }

    @Override
    protected void addBootOperations(List<ModelNode> bootOperations) throws Exception {
        bootOperations.add(OutboundConnectionAddBuilder.builder(CONNECTION_NAME)
                .setUrl("ldap://" + HOST_NAME + ":" + LDAP_PORT)
                .setSearchDn("uid=wildfly,dc=simple,dc=wildfly,dc=org")
                .setSearchCredential("wildfly_password")
                .build());
        // The super implementation of this method calls initialiseRealm for the realm
        // being used for testing, however the connection to the LDAP server should be
        // defined here.
        super.addBootOperations(bootOperations);

        // We define a second realm here as well.
        bootOperations.add(SecurityRealmAddBuilder.builder(ADVANCED_REALM)
                .authentication().ldap()
                .setConnection(CONNECTION_NAME)
                .setBaseDn(BASE_DN)
                .setAdvancedFilter(ADVANCED_FILTER)
                .build().build().build());
    }

    @Override
    protected void initialiseRealm(SecurityRealmAddBuilder builder) throws Exception {
        builder.authentication()
                .ldap()
                .setConnection(CONNECTION_NAME)
                .setBaseDn(BASE_DN)
                .setUsernameFilter(USERNAME_FILTER)
                .build().build();
    }

}
