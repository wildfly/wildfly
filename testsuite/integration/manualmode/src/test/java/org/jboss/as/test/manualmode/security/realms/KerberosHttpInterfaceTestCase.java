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
package org.jboss.as.test.manualmode.security.realms;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.annotations.CreateKdcServer;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.AnnotationUtils;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import static org.jboss.as.test.integration.management.base.AbstractCliTestBase.closeCLI;
import static org.jboss.as.test.integration.management.base.AbstractCliTestBase.initCLI;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.integration.security.common.AbstractKrb5ConfServerSetupTask;
import org.jboss.as.test.integration.security.common.KDCServerAnnotationProcessor;
import org.jboss.as.test.integration.security.common.Krb5LoginConfiguration;
import org.jboss.as.test.integration.security.common.ManagedCreateLdapServer;
import org.jboss.as.test.integration.security.common.ManagedCreateTransport;
import org.jboss.as.test.integration.security.common.NullHCCredentials;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.negotiation.JBossNegotiateSchemeFactory;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.logging.Logger;
import org.jboss.security.auth.callback.UsernamePasswordHandler;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for Kerberos auth for management over HTTP.
 *
 * @author olukas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class KerberosHttpInterfaceTestCase extends AbstractCliTestBase {
    
    private static final Logger LOGGER = Logger.getLogger(KerberosHttpInterfaceTestCase.class);
    
    private static final String DEFAULT_JBOSSAS = "default-jbossas";

    private static final String BATCH_CLI_FILENAME = "kerberos-http-interface-batch.cli";
    private static final String REMOVE_BATCH_CLI_FILENAME = "kerberos-http-interface-remove-batch.cli";

    private static final File WORK_DIR = new File("kerberos-http-interface" + System.currentTimeMillis());
    private static final File BATCH_CLI_FILE = new File(WORK_DIR, BATCH_CLI_FILENAME);
    private static final File REMOVE_BATCH_CLI_FILE = new File(WORK_DIR, REMOVE_BATCH_CLI_FILENAME);
    private static final String TEST_NAME = KerberosHttpInterfaceTestCase.class.getSimpleName();
    private static final String ROLES_FILENAME = TEST_NAME + "-roles.properties";
    private static final String USERS_FILENAME = TEST_NAME + "-users.properties";
    private static final File ROLES_FILE = new File(WORK_DIR, ROLES_FILENAME);
    private static final File USERS_FILE = new File(WORK_DIR, USERS_FILENAME);
    
    private static final int KERBEROS_PORT1 = 6088;
    private static final int KERBEROS_PORT2 = 6188;
    private static final int LDAP_PORT1 = 10389;
    private static final int LDAP_PORT2 = 10489;
    
    private static final int MGMT_PORT = 9990;
    private static final String MGMT_CTX = "/management";
    
    private static final String MANAGEMENT_OP_EXPECTED_RESULT = "\"running\"";
    
    private static final String USER = "hnelson";
    private static final String REALM = "JBOSS.ORG";
    private static final String USERNAME = USER + "@" + REALM;
    private static final String PASSWORD = "secret";
    
    private static String hostname = null;
    private static String originalRealm = null;
    
    
    @ArquillianResource
    private static ContainerController container;
    
    private static final Krb5ConfServerSetupTask krb5ConfServerSetupTask = new Krb5ConfServerSetupTask();
    private static final DirectoryServerSetupTask directoryServerSetupTask = new DirectoryServerSetupTask();
    private static final Directory2ServerSetupTask directoryServerSetupTask2 = new Directory2ServerSetupTask();
    
    /**
     * Initialize servers and appropriate settings.
     * 
     * @throws Exception 
     */
    @Test
    @InSequence(Integer.MIN_VALUE)
    public void initServer() throws Exception {
        
        container.start(DEFAULT_JBOSSAS);

        WORK_DIR.mkdirs();
        
        ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();        
        ManagementClient managementClient = null;
        try {
            managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                    TestSuiteEnvironment.getServerPort());

            krb5ConfServerSetupTask.setup(managementClient, DEFAULT_JBOSSAS);
            directoryServerSetupTask.setup(managementClient, DEFAULT_JBOSSAS);
            directoryServerSetupTask2.setup(managementClient, DEFAULT_JBOSSAS);

            hostname = NetworkUtils.formatPossibleIpv6Address(Utils.getCannonicalHost(managementClient));
        } finally {
            if (managementClient!=null)
                managementClient.close();
            if (client!=null) 
                client.close();
        }
        
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream(ROLES_FILENAME), ROLES_FILE);
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream(USERS_FILENAME), USERS_FILE);
        
        final Map<String, String> map = new HashMap<String, String>();
        map.put("keyTabAbsolutePath", escapePath(Krb5ConfServerSetupTask.getKeyTabFullPath()));
        map.put("keyTabRelativePath", escapePath(Krb5ConfServerSetupTask.getWorkDirAbsolutePath()));
        map.put("wrongKeyTabAbsolutePath", escapePath(Krb5ConfServerSetupTask.getKeyTabFullPath()) + "wrong");
        map.put("krbConfFile", escapePath(Krb5ConfServerSetupTask.getKrb5ConfFullPath()));
        map.put("serverKeyTabName", Krb5ConfServerSetupTask.HTTP_KEYTAB_FILE.getName());
        map.put("hostname", hostname);
        map.put("usersProperties", escapePath(USERS_FILE.getAbsolutePath()));
        map.put("rolesProperties", escapePath(ROLES_FILE.getAbsolutePath()));
        FileUtils.write(BATCH_CLI_FILE,
                StrSubstitutor.replace(IOUtils.toString(getClass().getResourceAsStream(BATCH_CLI_FILENAME), "UTF-8"), map),
                "UTF-8");
        
        FileUtils.write(REMOVE_BATCH_CLI_FILE, StrSubstitutor.replace(
                IOUtils.toString(getClass().getResourceAsStream(REMOVE_BATCH_CLI_FILENAME), "UTF-8"), map), "UTF-8");

        initCLI();
        cli.sendLine("/core-service=management/management-interface=http-interface:read-attribute(name=security-realm)");
        CLIOpResult result = cli.readAllAsOpResult();
        originalRealm = (String)result.getResult();
        final boolean batchResult = runBatch(BATCH_CLI_FILE);
        closeCLI();
        
        try {
            assertTrue("Server configuration failed", batchResult);
        } finally {
            container.stop(DEFAULT_JBOSSAS);
        }
        container.start(DEFAULT_JBOSSAS);
        initCLI();
        
    }
    
    /**
     * Test whether user with valid Kerberos ticket has granted access to management over HTTP when only one keytab element 
     * (with correct settings) is used in server configuration.
     * 
     * @throws Exception 
     */
    @Test
    @InSequence(1)
    public void testMinimalKeyTab() throws Exception {
        // https://bugzilla.redhat.com/show_bug.cgi?id=1161589
        Assume.assumeTrue(!System.getProperty("java.vendor").toUpperCase(Locale.ENGLISH).contains("IBM"));
        setHttpInterfaceRealm("MinimalKeyTab");     
        String response = makeCallWithKerberosAuthn(createSimpleManagementOperationURI(), USERNAME, PASSWORD, 200);
        assertEquals("Unexpected response from HTTP management interface", MANAGEMENT_OP_EXPECTED_RESULT, response);
    }
    
    /**
     * Test whether user with valid Kerberos ticket has granted access to management over HTTP when three keytab elements (and 
     * one of them has correct settings and appropriate for-hosts attribute) is used in server configuration.
     * 
     * @throws Exception 
     */
    @Test
    @InSequence(2)
    public void testForHostKeyTab() throws Exception {
        // https://bugzilla.redhat.com/show_bug.cgi?id=1161589
        Assume.assumeTrue(!System.getProperty("java.vendor").toUpperCase(Locale.ENGLISH).contains("IBM"));
        setHttpInterfaceRealm("ThreeKeyTabs");        
        String response = makeCallWithKerberosAuthn(createSimpleManagementOperationURI(), USERNAME, PASSWORD, 200);
        assertEquals("Unexpected response from HTTP management interface", MANAGEMENT_OP_EXPECTED_RESULT, response);
    }
    
    /**
     * Test whether user with valid Kerberos ticket has granted access to management over HTTP when three keytab elements (none 
     * of them has appropriate for-hosts, then asterisk with correct setting take into account) is used in server configuration.
     * 
     * @throws Exception 
     */
    @Test
    @InSequence(3)
    public void testForHostAsteriskKeyTab() throws Exception {
        // https://bugzilla.redhat.com/show_bug.cgi?id=1161589
        Assume.assumeTrue(!System.getProperty("java.vendor").toUpperCase(Locale.ENGLISH).contains("IBM"));
        setHttpInterfaceRealm("AsteriskKeyTab");        
        String response = makeCallWithKerberosAuthn(createSimpleManagementOperationURI(), USERNAME, PASSWORD, 200);
        assertEquals("Unexpected response from HTTP management interface", MANAGEMENT_OP_EXPECTED_RESULT, response);
    }
    
    /**
     * Test whether user with valid Kerberos ticket has not granted access to management over HTTP when there is no suitable 
     * keytab element in server configuration.
     * 
     * @throws Exception 
     */
    @Test
    @InSequence(4)
    public void testNotSuitableKeyTab() throws Exception {
        // https://bugzilla.redhat.com/show_bug.cgi?id=1161589
        Assume.assumeTrue(!System.getProperty("java.vendor").toUpperCase(Locale.ENGLISH).contains("IBM"));
        setHttpInterfaceRealm("NotSuitableKeyTab");        
        makeCallWithKerberosAuthn(createSimpleManagementOperationURI(), USERNAME, PASSWORD, 403);
    }
    
    /**
     * Test whether correct keytab is used when there is no appropriate for-hosts attribute value.
     * 
     * @throws Exception 
     */
    @Test
    @InSequence(5)
    public void testAppropriateHostNotInForHostKeyTab() throws Exception {
        // https://bugzilla.redhat.com/show_bug.cgi?id=1161589
        Assume.assumeTrue(!System.getProperty("java.vendor").toUpperCase(Locale.ENGLISH).contains("IBM"));
        setHttpInterfaceRealm("AppropriateHostNotInForHostKeyTab");        
        String response = makeCallWithKerberosAuthn(createSimpleManagementOperationURI(), USERNAME, PASSWORD, 200);
        assertEquals("Unexpected response from HTTP management interface", MANAGEMENT_OP_EXPECTED_RESULT, response);
    }
    
    /**
     * Test whether user with valid Kerberos ticket has not granted access to management over HTTP when there is wrong principal 
     * name in principal attribute in keytab.
     * 
     * @throws Exception 
     */
    @Test
    @InSequence(6)
    public void testWrongPrincipalKeyTab() throws Exception {
        // https://bugzilla.redhat.com/show_bug.cgi?id=1161589
        Assume.assumeTrue(!System.getProperty("java.vendor").toUpperCase(Locale.ENGLISH).contains("IBM"));
        setHttpInterfaceRealm("WrongPrincipalKeyTab");        
        makeCallWithKerberosAuthn(createSimpleManagementOperationURI(), USERNAME, PASSWORD, 403);
    }
    
    /**
     * Test whether user with valid Kerberos ticket has not granted access to management over HTTP when there is wrong realm in 
     * principal attribute in keytab.
     * 
     * @throws Exception 
     */
    @Test
    @InSequence(7)
    public void testWrongRealmKeyTab() throws Exception {
        // https://bugzilla.redhat.com/show_bug.cgi?id=1161589
        Assume.assumeTrue(!System.getProperty("java.vendor").toUpperCase(Locale.ENGLISH).contains("IBM"));
        setHttpInterfaceRealm("WrongRealmKeyTab");        
        makeCallWithKerberosAuthn(createSimpleManagementOperationURI(), USERNAME, PASSWORD, 403);
    }
    
    /**
     * Test whether relative-to attribute works as expected.
     * 
     * @throws Exception 
     */
    @Test
    @InSequence(8)
    public void testRelativeToKeyTab() throws Exception {
        // https://bugzilla.redhat.com/show_bug.cgi?id=1161589
        Assume.assumeTrue(!System.getProperty("java.vendor").toUpperCase(Locale.ENGLISH).contains("IBM"));
        setHttpInterfaceRealm("RelativeToKeyTab");        
        String response = makeCallWithKerberosAuthn(createSimpleManagementOperationURI(), USERNAME, PASSWORD, 200);
        assertEquals("Unexpected response from HTTP management interface", MANAGEMENT_OP_EXPECTED_RESULT, response);
    }
    
    /**
     * Test whether realm part of username is not stripped when remove-realm=false attribute is used.
     * 
     * @throws Exception 
     */
    @Test
    @InSequence(9)
    public void testDoNotRemoveKerberosRealm() throws Exception {
        // https://bugzilla.redhat.com/show_bug.cgi?id=1161589
        Assume.assumeTrue(!System.getProperty("java.vendor").toUpperCase(Locale.ENGLISH).contains("IBM"));
        setHttpInterfaceRealm("DoNotRemoveKerberosRealm");        
        callWhoAmIWithKerberosAuthn(createManagementURI(), USERNAME, PASSWORD, USERNAME);
    }
    
    /**
     * Test whether realm part of username is stripped when remove-realm=true attribute is used.
     * 
     * @throws Exception 
     */
    @Test
    @InSequence(10)
    public void testRemoveKerberosRealm() throws Exception {
        // https://bugzilla.redhat.com/show_bug.cgi?id=1161589
        Assume.assumeTrue(!System.getProperty("java.vendor").toUpperCase(Locale.ENGLISH).contains("IBM"));
        setHttpInterfaceRealm("RemoveKerberosRealm");        
        callWhoAmIWithKerberosAuthn(createManagementURI(), USERNAME, PASSWORD, USER);
    }

    /**
     * Test whether user with valid Kerberos ticket from different KDC has not granted access to management over HTTP.
     * 
     * @throws Exception 
     */
    @Test
    @InSequence(11)
    public void testUserWithTicketFromDifferentRealm() throws Exception {
        // https://bugzilla.redhat.com/show_bug.cgi?id=1161589
        Assume.assumeTrue(!System.getProperty("java.vendor").toUpperCase(Locale.ENGLISH).contains("IBM"));
        setHttpInterfaceRealm("MinimalKeyTab");
        makeCallWithKerberosAuthn(createSimpleManagementOperationURI(), "hnelson@JBOSS.COM", PASSWORD, 401);
        setHttpInterfaceRealm("RemoveKerberosRealm");
        makeCallWithKerberosAuthn(createSimpleManagementOperationURI(), "hnelson@JBOSS.COM", PASSWORD, 401);
    }

    /**
     * Test whether wrong configuration of keytab element (wrong principal name part in principal attribute) leads to correct
     * fallback.
     * 
     * @throws Exception 
     */
    @Test
    @InSequence(12)
    public void testFallBackInWrong1KerberosRealm() throws Exception {
        // https://bugzilla.redhat.com/show_bug.cgi?id=1161589
        Assume.assumeTrue(!System.getProperty("java.vendor").toUpperCase(Locale.ENGLISH).contains("IBM"));
        setHttpInterfaceRealm("FallBackWrong1KerberosRealm");
        String response = makeCallWithKerberosAuthnWithFallBack(createSimpleManagementOperationURI(), USERNAME, PASSWORD, 401, 
                "admin", "admin", 200);
        assertEquals("Unexpected response from HTTP management interface", MANAGEMENT_OP_EXPECTED_RESULT, response);
        response = makeCallWithoutKerberosAuthnTicketWithFallback(createSimpleManagementOperationURI(), "admin", "admin", 200);
        assertEquals("Unexpected response from HTTP management interface", MANAGEMENT_OP_EXPECTED_RESULT, response);
    }
    
    /**
     * Test whether wrong configuration of keytab element (wrong realm part in principal attribute) leads to correct fallback.
     * 
     * @throws Exception 
     */
    @Test
    @InSequence(13)
    public void testFallBackInWrong2KerberosRealm() throws Exception {
        // https://bugzilla.redhat.com/show_bug.cgi?id=1161589
        Assume.assumeTrue(!System.getProperty("java.vendor").toUpperCase(Locale.ENGLISH).contains("IBM"));
        setHttpInterfaceRealm("FallBackWrong2KerberosRealm");
        String response = makeCallWithKerberosAuthnWithFallBack(createSimpleManagementOperationURI(), USERNAME, PASSWORD, 401, 
                "admin", "admin", 200);
        assertEquals("Unexpected response from HTTP management interface", MANAGEMENT_OP_EXPECTED_RESULT, response);
        response = makeCallWithoutKerberosAuthnTicketWithFallback(createSimpleManagementOperationURI(), "admin", "admin", 200);
        assertEquals("Unexpected response from HTTP management interface", MANAGEMENT_OP_EXPECTED_RESULT, response);
    }
    
    /**
     * Test whether wrong configuration of keytab element (wrong path to keytab) leads to correct fallback.
     * 
     * @throws Exception 
     */
    @Test
    @InSequence(14)
    public void testFallBackInWrong3KerberosRealm() throws Exception {
        // https://bugzilla.redhat.com/show_bug.cgi?id=1161589
        Assume.assumeTrue(!System.getProperty("java.vendor").toUpperCase(Locale.ENGLISH).contains("IBM"));
        setHttpInterfaceRealm("FallBackWrong3KerberosRealm");
        String response = makeCallWithKerberosAuthnWithFallBack(createSimpleManagementOperationURI(), USERNAME, PASSWORD, 401, 
                "admin", "admin", 200);
        assertEquals("Unexpected response from HTTP management interface", MANAGEMENT_OP_EXPECTED_RESULT, response);
        response = makeCallWithoutKerberosAuthnTicketWithFallback(createSimpleManagementOperationURI(), "admin", "admin", 200);
        assertEquals("Unexpected response from HTTP management interface", MANAGEMENT_OP_EXPECTED_RESULT, response);
    }
    
    /**
     * Test whether :test() operation in CLI works as expected.
     * 
     * @throws Exception 
     */
    @Test
    @InSequence(15)
    public void testTestOperationInKerberosRealm() throws Exception {
        // https://bugzilla.redhat.com/show_bug.cgi?id=1161589
        Assume.assumeTrue(!System.getProperty("java.vendor").toUpperCase(Locale.ENGLISH).contains("IBM"));
        setHttpInterfaceRealm("ThreeKeyTabs");
        assertTrue("Result of test() operation does not contain Private Credentials with Kerberos Principal.",
                testTestOperationContainKerberosPrincipal("ThreeKeyTabs","HTTP\\/"+hostname+"@JBOSS.ORG"));
        assertFalse("Result of test() operation incorrectly contain Private Credentials with Kerberos Principal.",
                testTestOperationContainKerberosPrincipal("ThreeKeyTabs","HTTP\\/wronghost@JBOSS.ORG"));
    }

    /**
     * Test whether fallback is take into account when Kerberos server is down.
     * 
     * THIS TEST ALSO SHUT DOWN KERBEROS SERVER!
     * 
     * @throws Exception 
     */
    @Test
    @InSequence(Integer.MAX_VALUE - 2)
    public void testFallBackInKerberosRealm() throws Exception {
        // https://bugzilla.redhat.com/show_bug.cgi?id=1161589
        Assume.assumeTrue(!System.getProperty("java.vendor").toUpperCase(Locale.ENGLISH).contains("IBM"));
        setHttpInterfaceRealm("FallBackKerberosRealm");
        String response = makeCallWithKerberosAuthnWithFallBack(createSimpleManagementOperationURI(), USERNAME, PASSWORD, 401, 
                "admin", "admin", 200, true);
        assertEquals("Unexpected response from HTTP management interface", MANAGEMENT_OP_EXPECTED_RESULT, response);
    }
    
    /**
     * Revert the AS configuration and stop the server as the last but one step.
     *
     * @throws Exception
     */
    @Test
    @InSequence(Integer.MAX_VALUE - 1)
    public void closeServer() throws Exception {
        
        // https://bugzilla.redhat.com/show_bug.cgi?id=1161589
        // stop directories servers - only for IBM java, other jdks stop it in testFallBackInKerberosRealm()
        if (System.getProperty("java.vendor").toUpperCase(Locale.ENGLISH).contains("IBM")) {
            ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
                ManagementClient managementClient = null;
                try {
                    managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                            TestSuiteEnvironment.getServerPort());
                    directoryServerSetupTask.tearDown(managementClient, DEFAULT_JBOSSAS);
                    directoryServerSetupTask2.tearDown(managementClient, DEFAULT_JBOSSAS);
                    krb5ConfServerSetupTask.tearDown(managementClient, DEFAULT_JBOSSAS);
                } finally {
                    if (managementClient!=null)
                        managementClient.close();
                    if (client!=null) 
                        client.close();
                }
        }
        
        assertTrue(container.isStarted(DEFAULT_JBOSSAS));
        
        setHttpInterfaceRealm(originalRealm);

        final boolean batchResult = runBatch(REMOVE_BATCH_CLI_FILE);
        closeCLI();
        container.stop(DEFAULT_JBOSSAS);

        FileUtils.deleteQuietly(WORK_DIR);

        assertTrue("Reverting server configuration failed", batchResult);
    }
    
    /**
     * Escapes backslashes in the file path.
     * @param path
     * @return
     */
    private static String escapePath(String path) {
        return path.replace("\\", "\\\\");
    }
    
    /**
     * Runs a CLI script in as a batch.
     *
     * @param batchFile CLI file
     * @return true if CLI returns Success
     * @throws IOException
     */
    private static boolean runBatch(File batchFile) throws IOException {
        cli.sendLine("run-batch --file=\"" + batchFile.getAbsolutePath()
                + "\" --headers={allow-resource-service-restart=true} -v", false);
        return cli.readAllAsOpResult().isIsOutcomeSuccess();
    }
    
    /**
     * Set http-interface realm.
     * 
     * @param realm new http-interface realm
     * @throws IOException 
     */
    private void setHttpInterfaceRealm(String realm) throws IOException {
        cli.sendLine("/core-service=management/management-interface=http-interface:write-attribute(name=security-realm,value=" +
                realm + ")");
        CLIOpResult result = cli.readAllAsOpResult();
        assertTrue("Reading cache data using management API failed", result.isIsOutcomeSuccess());
    }
    
    /**
     * Create URI for read server state management operation.
     * 
     * @return URI for read server state management operation
     * @throws URISyntaxException
     * @throws IOException 
     */
    private URI createSimpleManagementOperationURI() throws URISyntaxException, IOException {
        return createManagementOperationURI(":" + MGMT_PORT + MGMT_CTX + "?operation=attribute&name=server-state");
    }
    
    /**
     * Create URI for management operation.
     * 
     * @return URI for management operation
     * @throws URISyntaxException
     * @throws IOException 
     */
    private URI createManagementURI() throws URISyntaxException, IOException {
        return createManagementOperationURI(":" + MGMT_PORT + MGMT_CTX);
    }
    
    /**
     * Create URI for wanted management operation.
     * 
     * @param op wanted management operation
     * @return URI for wanted management operation
     * @throws URISyntaxException
     * @throws IOException 
     */
    private URI createManagementOperationURI(String op) throws URISyntaxException, IOException {
        ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient(); 
        ManagementClient managementClient = null;
        try {
            managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                    TestSuiteEnvironment.getServerPort());
        return new URI("http://" + NetworkUtils.formatPossibleIpv6Address(Utils.getCannonicalHost(managementClient)) + op);
        } finally {
            if (managementClient!=null)
                managementClient.close();
            if (client!=null) 
                client.close();
        }        
    }
    
    /**
     * Check whether :test() operation contains Kerberos Principal.
     * 
     * @param realm security-realm
     * @param principal keytab principal
     * @return true if :test() operation contains Kerberos Principal, false otherwise
     * @throws IOException 
     */
    private boolean testTestOperationContainKerberosPrincipal(String realm, String principal) throws IOException {
        if (principal.contains("[")||principal.contains("]")) {
            principal = principal.replace(":", "\\:");
        }
        cli.sendLine("/core-service=management/security-realm=" + realm +"/server-identity=kerberos/keytab=" + principal 
                +":test()",true);
        CLIOpResult result = cli.readAllAsOpResult();
        if (result.isIsOutcomeSuccess()) {
            String textResult = (String)result.getNamedResult("subject");        
            return textResult.contains("Kerberos Principal");
        } else {
            return false;
        }
    }

    /**
     * Returns response body for the given URL request as a String. It also checks if the returned HTTP status code is the
     * expected one. 
     *
     * @param uri URI to which the request should be made
     * @param user Username
     * @param pass Password
     * @param expectedStatusCode expected status code returned from the requested server
     * @return HTTP response body
     * @throws IOException
     * @throws URISyntaxException
     * @throws PrivilegedActionException
     * @throws LoginException
     */
    private static String makeCallWithKerberosAuthn(final URI uri, final String user, final String pass,
            final int expectedStatusCode) throws IOException, URISyntaxException, PrivilegedActionException, LoginException {
        LOGGER.info("Requesting URI: " + uri);
        final DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            httpClient.getAuthSchemes().register(AuthPolicy.SPNEGO, new JBossNegotiateSchemeFactory(true));
            httpClient.getCredentialsProvider().setCredentials(new AuthScope(null, -1, null), new NullHCCredentials());

            final HttpGet httpGet = new HttpGet(uri);
            final HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            final HttpEntity entity = response.getEntity();
            if (HttpServletResponse.SC_UNAUTHORIZED != statusCode) {
                if (expectedStatusCode!=HttpServletResponse.SC_FORBIDDEN)
                    fail("Unauthorized access to protected page returned " + statusCode + " instead of 401.");
            } else {
                final Header[] authnHeaders = response.getHeaders("WWW-Authenticate");
                assertTrue("WWW-Authenticate header is not present", authnHeaders != null && authnHeaders.length > 0);
                final Set<String> authnHeaderValues = new HashSet<String>();
                for (final Header header : authnHeaders) {
                    authnHeaderValues.add(header.getValue());
                }
                assertTrue("WWW-Authenticate: Negotiate header is missing", authnHeaderValues.contains("Negotiate"));

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("HTTP response was SC_UNAUTHORIZED, let's authenticate the user " + user);
                }
            }
            if (entity != null)
                EntityUtils.consume(entity);

            // Use our custom configuration to avoid reliance on external config
            Configuration.setConfiguration(new Krb5LoginConfiguration());
            // 1. Authenticate to Kerberos.
            final LoginContext lc = new LoginContext(Utils.class.getName(), new UsernamePasswordHandler(user, pass));
            lc.login();
            // 2. Perform the work as authenticated Subject.
            final String responseBody = Subject.doAs(lc.getSubject(), new PrivilegedExceptionAction<String>() {
                @Override
                public String run() throws Exception {
                    final HttpResponse response = httpClient.execute(httpGet);
                    int statusCode = response.getStatusLine().getStatusCode();
                    assertEquals("Unexpected status code returned after the authentication.", expectedStatusCode, statusCode);
                    return EntityUtils.toString(response.getEntity());
                }
            });
            lc.logout();
            return responseBody;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }
    
    /**
     * Run whoAmI operation and test whether result is as expected.
     * 
     * @param uri URI to which the request should be made
     * @param user Username
     * @param pass Password
     * @param expectedUser expected user
     * @return String result of whoAmI operation
     * @throws IOException
     * @throws URISyntaxException
     * @throws PrivilegedActionException
     * @throws LoginException 
     */
    private static String callWhoAmIWithKerberosAuthn(final URI uri, final String user, final String pass,
            final String expectedUser) throws IOException, URISyntaxException, PrivilegedActionException, LoginException {
        LOGGER.info("Requesting URI: " + uri);
        final DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            httpClient.getAuthSchemes().register(AuthPolicy.SPNEGO, new JBossNegotiateSchemeFactory(true));
            httpClient.getCredentialsProvider().setCredentials(new AuthScope(null, -1, null), new NullHCCredentials());

            final HttpPost httpPost = new HttpPost(uri);
            StringEntity params =new StringEntity("{\"operation\":\"whoami\",\"include-runtime\":\"true\","
                    + "\"address\":[],\"json.pretty\":1} ");
            httpPost.addHeader("content-type", "application/json");
            httpPost.setEntity(params);

            // Use our custom configuration to avoid reliance on external config
            Configuration.setConfiguration(new Krb5LoginConfiguration());
            // 1. Authenticate to Kerberos.
            final LoginContext lc = new LoginContext(Utils.class.getName(), new UsernamePasswordHandler(user, pass));
            lc.login();
            // 2. Perform the work as authenticated Subject.
            final String responseBody = Subject.doAs(lc.getSubject(), new PrivilegedExceptionAction<String>() {
                @Override
                public String run() throws Exception {
                    final HttpResponse response = httpClient.execute(httpPost);
                    int statusCode = response.getStatusLine().getStatusCode();
                    assertEquals("Unexpected status code returned after the authentication.", HttpServletResponse.SC_OK, 
                            statusCode);
                    String jsonResponse = EntityUtils.toString(response.getEntity());
                    assertTrue("Expected username " + expectedUser +" is not contain in result of whoAmI operation.", 
                            jsonResponse.contains("\"" + expectedUser + "\""));
                    return jsonResponse;
                }
            });
            lc.logout();
            return responseBody;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }
    
    /**
     * Try to authenticate with Kerberos ticket and then falls into fallback. Authentication with Kerberos ticket is expected 
     * to be unsuccessful.
     * 
     * @param uri URI to which the request should be made
     * @param user user
     * @param pass password
     * @param expectedStatusCode expected status code
     * @param fallbackUser user for fallback
     * @param fallbackPass password for fallback
     * @param expectedFallbackStatusCode expected status after fallback
     * @return
     * @throws Exception 
     */
    private static String makeCallWithKerberosAuthnWithFallBack(final URI uri, final String user, final String pass, 
            final int expectedStatusCode, final String fallbackUser, final String fallbackPass, 
            final int expectedFallbackStatusCode) throws Exception {
        return makeCallWithKerberosAuthnWithFallBack(uri, user, pass, expectedStatusCode, fallbackUser, fallbackPass, 
                expectedFallbackStatusCode, false);
    }
    
    /**
     * Try to authenticate with Kerberos ticket and then falls into fallback. Authentication with Kerberos ticket is expected 
     * to be unsuccessful. If stopKerberos is set to true it also stop Kerberos server before authentication.
     * 
     * @param uri URI to which the request should be made
     * @param user user
     * @param pass password
     * @param expectedStatusCode expected status code
     * @param fallbackUser user for fallback
     * @param fallbackPass password for fallback
     * @param expectedFallbackStatusCode expected status after fallback
     * @param stopKerberos true for stop Kerberos server before authentication, false otherwise
     * @return
     * @throws Exception 
     */
    private static String makeCallWithKerberosAuthnWithFallBack(final URI uri, final String user, final String pass, 
            final int expectedStatusCode, final String fallbackUser, final String fallbackPass, 
            final int expectedFallbackStatusCode, boolean stopKerberos) throws Exception {
        LOGGER.info("Requesting URI: " + uri);
        final DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            httpClient.getAuthSchemes().register(AuthPolicy.SPNEGO, new JBossNegotiateSchemeFactory(true));
            
            final HttpGet httpGet = new HttpGet(uri);

            // Use our custom configuration to avoid reliance on external config
            Configuration.setConfiguration(new Krb5LoginConfiguration());
            // 1. Authenticate to Kerberos.
            final LoginContext lc = new LoginContext(Utils.class.getName(), new UsernamePasswordHandler(user, pass));
            lc.login();
            
            if (stopKerberos) {
                ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
                ManagementClient managementClient = null;
                try {
                    managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                            TestSuiteEnvironment.getServerPort());
                    directoryServerSetupTask.tearDown(managementClient, DEFAULT_JBOSSAS);
                    directoryServerSetupTask2.tearDown(managementClient, DEFAULT_JBOSSAS);
                    krb5ConfServerSetupTask.tearDown(managementClient, DEFAULT_JBOSSAS);
                } finally {
                    if (managementClient!=null)
                        managementClient.close();
                    if (client!=null) 
                        client.close();
                }
            }
            
            // 2. Perform the work as authenticated Subject.
            String responseBody = Subject.doAs(lc.getSubject(), new PrivilegedExceptionAction<String>() {
                @Override
                public String run() throws Exception {
                    final HttpResponse response = httpClient.execute(httpGet);
                    int statusCode = response.getStatusLine().getStatusCode();
                    assertEquals("Unexpected status code returned after the authentication without fallback.", 
                            expectedStatusCode, statusCode);
                    return EntityUtils.toString(response.getEntity());
                }
            });
            
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(fallbackUser, fallbackPass);
            httpClient.getCredentialsProvider().setCredentials(new AuthScope(uri.getHost(), uri.getPort()), credentials);

            responseBody = Subject.doAs(lc.getSubject(), new PrivilegedExceptionAction<String>() {
                @Override
                public String run() throws Exception {
                    final HttpResponse response = httpClient.execute(httpGet);
                    int statusCode = response.getStatusLine().getStatusCode();
                    assertEquals("Unexpected status code returned after the authentication after fallback.", 
                            expectedFallbackStatusCode, statusCode);
                    return EntityUtils.toString(response.getEntity());
                }
            });
            lc.logout();            
            
            return responseBody;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }
    
    /**
     * It represents situation when access is done without any Kerberos Ticket. For that reason fallback is taken into account.
     * 
     * @param uri URI to which the request should be made
     * @param fallbackUser user for fallback
     * @param fallbackPass password for fallback
     * @param expectedStatusCode expected status code
     * @return
     * @throws IOException
     * @throws URISyntaxException 
     */
    private static String makeCallWithoutKerberosAuthnTicketWithFallback(final URI uri, final String fallbackUser, 
            final String fallbackPass,
            final int expectedStatusCode) throws IOException, URISyntaxException {
        LOGGER.info("Requesting URI: " + uri);
        final DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            httpClient.getAuthSchemes().register(AuthPolicy.SPNEGO, new JBossNegotiateSchemeFactory(true));
            
            final HttpGet httpGet = new HttpGet(uri);
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(fallbackUser, fallbackPass);
            httpClient.getCredentialsProvider().setCredentials(new AuthScope(uri.getHost(), uri.getPort()), credentials);
            
            final HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            assertEquals("Unexpected status code returned.", expectedStatusCode, statusCode);
            return EntityUtils.toString(response.getEntity());
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }
    
    
    static class Krb5ConfServerSetupTask extends AbstractKrb5ConfServerSetupTask {
        
        @Override
        protected List<UserForKeyTab> kerberosUsers() {
            ArrayList<UserForKeyTab> users = new ArrayList<UserForKeyTab>();
            users.add(new UserForKeyTab("hnelson@JBOSS.ORG", "secret", new File(WORK_DIR,"hnelson.keytab")));
            return users;
        }
        
        private static String getWorkDirAbsolutePath() {
            return WORK_DIR.getAbsolutePath();
        }
        
    }
    
    /**
     * A server setup task which configures and starts Kerberos KDC server.
     */
    //@formatter:off
    @CreateDS(
        name = "JBossDS",
        partitions =
        {
            @CreatePartition(
                name = "jboss",
                suffix = "dc=jboss,dc=org",
                contextEntry = @ContextEntry(
                    entryLdif =
                        "dn: dc=jboss,dc=org\n" +
                        "dc: jboss\n" +
                        "objectClass: top\n" +
                        "objectClass: domain\n\n" ),
                indexes =
                {
                    @CreateIndex( attribute = "objectClass" ),
                    @CreateIndex( attribute = "dc" ),
                    @CreateIndex( attribute = "ou" )
                })
        },
        additionalInterceptors = { KeyDerivationInterceptor.class })
    @CreateLdapServer (
            transports =
            {
                @CreateTransport( protocol = "LDAP",  port = LDAP_PORT1)
            })
    @CreateKdcServer(primaryRealm = "JBOSS.ORG",
        kdcPrincipal = "krbtgt/JBOSS.ORG@JBOSS.ORG",
        searchBaseDn = "dc=jboss,dc=org",
        transports =
        {
            @CreateTransport(protocol = "UDP", port = KERBEROS_PORT1)
        })
    //@formatter:on
    static class DirectoryServerSetupTask implements ServerSetupTask {

        private DirectoryService directoryService;
        private KdcServer kdcServer;
        private LdapServer ldapServer;

        private boolean removeBouncyCastle = false;

        /**
         * Creates directory services, starts LDAP server and KDCServer
         *
         * @param managementClient
         * @param containerId
         * @throws Exception
         * @see org.jboss.as.arquillian.api.ServerSetupTask#setup(org.jboss.as.arquillian.container.ManagementClient,
         *      java.lang.String)
         */
        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            try {
                if(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                    Security.addProvider(new BouncyCastleProvider());
                    removeBouncyCastle = true;
                }
            } catch(SecurityException ex) {
                LOGGER.warn("Cannot register BouncyCastleProvider", ex);
            }
            directoryService = DSAnnotationProcessor.getDirectoryService();
            final String hostname = Utils.getCannonicalHost(managementClient);
            final Map<String, String> map = new HashMap<String, String>();
            map.put("hostname", NetworkUtils.formatPossibleIpv6Address(hostname));
            final String secondaryTestAddress = Utils.getSecondaryTestAddress(managementClient, true);
            final String ldifContent = StrSubstitutor.replace(
                    IOUtils.toString(
                            KerberosHttpInterfaceTestCase.class.getResourceAsStream(KerberosHttpInterfaceTestCase.class
                                    .getSimpleName() + ".ldif"), "UTF-8"), map);
            LOGGER.info(ldifContent);
            
            final SchemaManager schemaManager = directoryService.getSchemaManager();
            try {
                for (LdifEntry ldifEntry : new LdifReader(IOUtils.toInputStream(ldifContent))) {
                    directoryService.getAdminSession().add(new DefaultEntry(schemaManager, ldifEntry.getEntry()));
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
            
            kdcServer = KDCServerAnnotationProcessor.getKdcServer(directoryService, KERBEROS_PORT1, hostname);
            
            final ManagedCreateLdapServer createLdapServer = new ManagedCreateLdapServer(
                    (CreateLdapServer) AnnotationUtils.getInstance(CreateLdapServer.class));
            fixTransportAddress(createLdapServer, secondaryTestAddress);
            
            ldapServer = ServerAnnotationProcessor.instantiateLdapServer(createLdapServer, directoryService);
            ldapServer.start();
        }

        /**
         * Fixes bind address in the CreateTransport annotation.
         *
         * @param createLdapServer
         */
        private void fixTransportAddress(ManagedCreateLdapServer createLdapServer, String address) {
            final CreateTransport[] createTransports = createLdapServer.transports();
            for (int i = 0; i < createTransports.length; i++) {
                final ManagedCreateTransport mgCreateTransport = new ManagedCreateTransport(createTransports[i]);
                mgCreateTransport.setAddress(address);
                createTransports[i] = mgCreateTransport;
            }
        }

        /**
         * Stops LDAP server and KDCServer and shuts down the directory service.
         *
         * @param managementClient
         * @param containerId
         * @throws Exception
         * @see org.jboss.as.arquillian.api.ServerSetupTask#tearDown(org.jboss.as.arquillian.container.ManagementClient,
         *      java.lang.String)
         */
        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            ldapServer.stop();
            kdcServer.stop();
            directoryService.shutdown();
            FileUtils.deleteDirectory(directoryService.getInstanceLayout().getInstanceDirectory());
            if(removeBouncyCastle) {
                try {
                    Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
                } catch(SecurityException ex) {
                    LOGGER.warn("Cannot deregister BouncyCastleProvider", ex);
                }
            }
        }

    }
    
    /**
     * A server setup task which configures and starts Kerberos KDC server.
     */
    //@formatter:off
    @CreateDS(
        name = "JBossDS2",
        partitions =
        {
            @CreatePartition(
                name = "jboss2",
                suffix = "dc=jboss,dc=com",
                contextEntry = @ContextEntry(
                    entryLdif =
                        "dn: dc=jboss,dc=com\n" +
                        "dc: jboss\n" +
                        "objectClass: top\n" +
                        "objectClass: domain\n\n" ),
                indexes =
                {
                    @CreateIndex( attribute = "objectClass" ),
                    @CreateIndex( attribute = "dc" ),
                    @CreateIndex( attribute = "ou" )
                })
        },
        additionalInterceptors = { KeyDerivationInterceptor.class })
    @CreateLdapServer (
            transports =
            {
                @CreateTransport( protocol = "LDAP",  port = LDAP_PORT2)
            })
    @CreateKdcServer(primaryRealm = "JBOSS.COM",
        kdcPrincipal = "krbtgt/JBOSS.COM@JBOSS.COM",
        searchBaseDn = "dc=jboss,dc=com",
        transports =
        {
            @CreateTransport(protocol = "UDP", port = KERBEROS_PORT2)
        })
    //@formatter:on
    static class Directory2ServerSetupTask implements ServerSetupTask {

        private DirectoryService directoryService;
        private KdcServer kdcServer;
        private LdapServer ldapServer;

        private boolean removeBouncyCastle = false;

        /**
         * Creates directory services, starts LDAP server and KDCServer
         *
         * @param managementClient
         * @param containerId
         * @throws Exception
         * @see org.jboss.as.arquillian.api.ServerSetupTask#setup(org.jboss.as.arquillian.container.ManagementClient,
         *      java.lang.String)
         */
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            try {
                if(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                    Security.addProvider(new BouncyCastleProvider());
                    removeBouncyCastle = true;
                }
            } catch(SecurityException ex) {
                LOGGER.warn("Cannot register BouncyCastleProvider", ex);
            }
            directoryService = DSAnnotationProcessor.getDirectoryService();
            final String hostname = Utils.getCannonicalHost(managementClient);
            final Map<String, String> map = new HashMap<String, String>();
            map.put("hostname", NetworkUtils.formatPossibleIpv6Address(hostname));
            final String secondaryTestAddress = Utils.getSecondaryTestAddress(managementClient, true);
            final String ldifContent = StrSubstitutor.replace(
                    IOUtils.toString(
                            KerberosHttpInterfaceTestCase.class.getResourceAsStream(KerberosHttpInterfaceTestCase.class
                                    .getSimpleName() + "2.ldif"), "UTF-8"), map);
            LOGGER.info(ldifContent);
            
            final SchemaManager schemaManager = directoryService.getSchemaManager();
            try {
                for (LdifEntry ldifEntry : new LdifReader(IOUtils.toInputStream(ldifContent))) {
                    directoryService.getAdminSession().add(new DefaultEntry(schemaManager, ldifEntry.getEntry()));
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
            
            kdcServer = KDCServerAnnotationProcessor.getKdcServer(directoryService, KERBEROS_PORT2, hostname);
            
            final ManagedCreateLdapServer createLdapServer = new ManagedCreateLdapServer(
                    (CreateLdapServer) AnnotationUtils.getInstance(CreateLdapServer.class));
            fixTransportAddress(createLdapServer, secondaryTestAddress);
            
            ldapServer = ServerAnnotationProcessor.instantiateLdapServer(createLdapServer, directoryService);
            ldapServer.start();
        }

        /**
         * Fixes bind address in the CreateTransport annotation.
         *
         * @param createLdapServer
         */
        private void fixTransportAddress(ManagedCreateLdapServer createLdapServer, String address) {
            final CreateTransport[] createTransports = createLdapServer.transports();
            for (int i = 0; i < createTransports.length; i++) {
                final ManagedCreateTransport mgCreateTransport = new ManagedCreateTransport(createTransports[i]);
                mgCreateTransport.setAddress(address);
                createTransports[i] = mgCreateTransport;
            }
        }

        /**
         * Stops LDAP server and KDCServer and shuts down the directory service.
         *
         * @param managementClient
         * @param containerId
         * @throws Exception
         * @see org.jboss.as.arquillian.api.ServerSetupTask#tearDown(org.jboss.as.arquillian.container.ManagementClient,
         *      java.lang.String)
         */
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            ldapServer.stop();
            kdcServer.stop();
            directoryService.shutdown();
            FileUtils.deleteDirectory(directoryService.getInstanceLayout().getInstanceDirectory());
            if(removeBouncyCastle) {
                try {
                    Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
                } catch(SecurityException ex) {
                    LOGGER.warn("Cannot deregister BouncyCastleProvider", ex);
                }
            }
        }

    }
}
