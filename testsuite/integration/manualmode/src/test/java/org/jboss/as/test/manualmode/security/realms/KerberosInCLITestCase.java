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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import static org.hamcrest.CoreMatchers.containsString;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.integration.management.util.CustomCLIExecutor;
import org.jboss.as.test.integration.security.common.AbstractKrb5ConfServerSetupTask;
import static org.jboss.as.test.integration.security.common.AbstractKrb5ConfServerSetupTask.HTTP_KEYTAB_FILE;
import org.jboss.as.test.integration.security.common.Utils;
import static org.jboss.as.test.manualmode.security.realms.KerberosRealmTestBase.escapePath;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.logging.Logger;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for Kerberos auth for Management CLI.
 *
 * @author olukas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class KerberosInCLITestCase extends KerberosRealmTestBase {
    
    private static final Logger LOGGER = Logger.getLogger(KerberosInCLITestCase.class);
    
    private static final String DEFAULT_JBOSSAS = "default-jbossas";

    private static final String BATCH_CLI_FILENAME = "kerberos-in-cli-batch.cli";
    private static final String REMOVE_BATCH_CLI_FILENAME = "kerberos-in-cli-remove-batch.cli";
    
    private static final File WORK_DIR = new File("kerberos-in-cli" + System.currentTimeMillis());
    private static final File BATCH_CLI_FILE = new File(WORK_DIR, BATCH_CLI_FILENAME);
    private static final File REMOVE_BATCH_CLI_FILE = new File(WORK_DIR, REMOVE_BATCH_CLI_FILENAME);
    private static final String ROLES_FILENAME = "roles.properties";
    private static final String USERS_FILENAME = "users.properties";
    private static final File ROLES_FILE = new File(WORK_DIR, ROLES_FILENAME);
    private static final File USERS_FILE = new File(WORK_DIR, USERS_FILENAME);
    private static final String JBOSS_CLI_FILE = "jboss-cli.xml";
    private static final File HOSTNAME_JBOSS_CLI_FILE = new File(WORK_DIR, "hostname-jboss-cli.xml");
    
    private static final String KRB5CC_FILENAME = "krb5cc";
    private static final File KRB5CC_FILE = new File(WORK_DIR, KRB5CC_FILENAME);
    private static final String KRB5CC_EMPTY_FILENAME = "krb5cc_empty";
    private static final File KRB5CC_EMPTY_FILE = new File(WORK_DIR, KRB5CC_EMPTY_FILENAME);
    private static final String KRB5CC_WRONG_REALM_FILENAME = "krb5cc_wrong_realm";
    private static final File KRB5CC_WRONG_REALM_FILE = new File(WORK_DIR, KRB5CC_WRONG_REALM_FILENAME);
    
    private static final String USER = "hnelson";
    private static final String REALM = "JBOSS.ORG";
    private static final String USERNAME = USER + "@" + REALM;
    
    private static final String JAAS_USER = "admin";
    private static final String JAAS_PASSWORD = "admin";
    
    private static String hostname = null;
    private static String originalRealm = null;
    
    private static final String RELOAD = "reload";
    private static final String WHOAMI = ":whoami";
    private static final int CLI_TIMEOUT = 60000;
    private static final int MAX_RELOAD_TIME = 30000;
    
    private static final List<String> JAAS_CLI_PARAMS = createJaasParams();
    private static final List<String> KRB_AND_JAAS_CLI_PARAMS = createKrbAndJaasParams();
    
    @ArquillianResource
    private static ContainerController container;
    
    private static final Krb5ConfServerSetupTask krb5ConfServerSetupTask = new Krb5ConfServerSetupTask();
    private static final DirectoryServerSetupTask directoryServerSetupTask = new DirectoryServerSetupTask();
    private static final Directory2ServerSetupTask directoryServerSetupTask2 = new Directory2ServerSetupTask();
    
    
    static class Krb5ConfServerSetupTask extends AbstractKrb5ConfServerSetupTask {
        
        @Override
        protected List<AbstractKrb5ConfServerSetupTask.UserForKeyTab> kerberosUsers() {
            ArrayList<AbstractKrb5ConfServerSetupTask.UserForKeyTab> users = new ArrayList<AbstractKrb5ConfServerSetupTask.UserForKeyTab>();
            users.add(new AbstractKrb5ConfServerSetupTask.UserForKeyTab("hnelson@JBOSS.ORG", "secret", new File(WORK_DIR,"hnelson.keytab")));
            return users;
        }
        
        @Override
        protected void createServerKeytab(String host) throws IOException {
            createKeytab("remote/" + host + "@JBOSS.ORG", "httppwd", HTTP_KEYTAB_FILE);
        }
        
    }
    
    /**
     * Initialize servers and appropriate settings.
     * 
     * @throws Exception 
     */
    @Test
    @InSequence(Integer.MIN_VALUE)
    public void initServer() throws Exception {
        Assume.assumeTrue(!System.getProperty("java.vendor").toUpperCase(Locale.ENGLISH).contains("IBM"));
        
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
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream(KRB5CC_FILENAME), KRB5CC_FILE);
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream(KRB5CC_EMPTY_FILENAME), KRB5CC_EMPTY_FILE);
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream(KRB5CC_WRONG_REALM_FILENAME), KRB5CC_WRONG_REALM_FILE);
        
        final Map<String, String> map = new HashMap<String, String>();
        map.put("hostname", hostname);
        FileUtils.write(HOSTNAME_JBOSS_CLI_FILE, StrSubstitutor.replace(
                IOUtils.toString(getClass().getResourceAsStream(JBOSS_CLI_FILE), "UTF-8"), map), "UTF-8");
        
        map.put("keyTabAbsolutePath", escapePath(Krb5ConfServerSetupTask.getKeyTabFullPath()));
        map.put("wrongKeyTabAbsolutePath", escapePath(Krb5ConfServerSetupTask.getKeyTabFullPath()) + "wrong");
        map.put("krbConfFile", escapePath(Krb5ConfServerSetupTask.getKrb5ConfFullPath()));
        map.put("usersProperties", escapePath(USERS_FILE.getAbsolutePath()));
        map.put("rolesProperties", escapePath(ROLES_FILE.getAbsolutePath()));
        FileUtils.write(BATCH_CLI_FILE,
                StrSubstitutor.replace(IOUtils.toString(getClass().getResourceAsStream(BATCH_CLI_FILENAME), "UTF-8"), map),
                "UTF-8");
        
        FileUtils.write(REMOVE_BATCH_CLI_FILE, StrSubstitutor.replace(
                IOUtils.toString(getClass().getResourceAsStream(REMOVE_BATCH_CLI_FILENAME), "UTF-8"), map), "UTF-8");

        initCLI();
        cli.sendLine("/core-service=management/management-interface=native-interface:read-attribute(name=security-realm)");
        CLIOpResult result = cli.readAllAsOpResult();
        originalRealm = (String)result.getResult();
        final boolean batchResult = runBatch(BATCH_CLI_FILE);
        closeCLI();
        assertTrue("Server configuration failed", batchResult);
        
        CustomCLIExecutor.execute(HOSTNAME_JBOSS_CLI_FILE, RELOAD);
        CustomCLIExecutor.waitForServerToReloadWithUserAndPassword(MAX_RELOAD_TIME, HOSTNAME_JBOSS_CLI_FILE, JAAS_USER, JAAS_PASSWORD);
        
    }
    
    /**
     * Test whether fall back is taken into account when path to keytab file in server configuration is wrong.
     * 
     * @throws Exception 
     */
    @Test
    @InSequence(2)
    public void testFallbackForWrongKeytab() throws Exception {
        Assume.assumeTrue(!System.getProperty("java.vendor").toUpperCase(Locale.ENGLISH).contains("IBM"));
        
        final String cliOutput = CustomCLIExecutor.execute(HOSTNAME_JBOSS_CLI_FILE, WHOAMI, KRB_AND_JAAS_CLI_PARAMS, 
                createKrbEnvironment(KRB5CC_FILE.getAbsolutePath()));
        assertThat("CLI output does not contain outcome success.", cliOutput, containsString("\"outcome\" => \"success\""));
        assertThat("Result of :whoami operation does not contain expected user.", cliOutput, containsString("\"" + JAAS_USER + "\""));
    }
    
    /**
     * Only set security realm for another tests.
     * 
     * @throws Exception 
     */
    @Test
    @InSequence(3)
    public void setTestKerberosRealm() throws Exception {
        Assume.assumeTrue(!System.getProperty("java.vendor").toUpperCase(Locale.ENGLISH).contains("IBM"));
        String setTestKerberosRealm = "/core-service=management/management-interface=native-interface:write-attribute"
                + "(name=security-realm,value=TestKerberosRealm)";
        CustomCLIExecutor.execute(HOSTNAME_JBOSS_CLI_FILE, setTestKerberosRealm, JAAS_CLI_PARAMS, null);
        CustomCLIExecutor.execute(HOSTNAME_JBOSS_CLI_FILE, RELOAD, JAAS_CLI_PARAMS, null);
        CustomCLIExecutor.waitForServerToReloadWithUserAndPassword(MAX_RELOAD_TIME, HOSTNAME_JBOSS_CLI_FILE, JAAS_USER, JAAS_PASSWORD);
    }
    
    /**
     * Test whether user with valid Kerberos ticket has granted access to Management CLI.
     * 
     * @throws Exception 
     */
    @Test
    @InSequence(4)
    public void testAccessWithCorrectTicket() throws Exception {
        Assume.assumeTrue(!System.getProperty("java.vendor").toUpperCase(Locale.ENGLISH).contains("IBM"));
        
        final String cliOutput = CustomCLIExecutor.execute(HOSTNAME_JBOSS_CLI_FILE, WHOAMI, KRB_AND_JAAS_CLI_PARAMS, 
                createKrbEnvironment(KRB5CC_FILE.getAbsolutePath()));
        assertThat("CLI output does not contain outcome success.", cliOutput, containsString("\"outcome\" => \"success\""));
        assertThat("Result of :whoami operation does not contain expected user.", cliOutput, containsString("\"" + USERNAME + "\""));
    }
    
    /**
     * Test whether user without valid Kerberos ticket in cache has not granted access to Management CLI. Wrong authentication 
     * with Kerberos is expected to fallback into JAAS authentication in this test.
     * 
     * @throws Exception 
     */
    @Test
    @InSequence(5)
    public void testAccessWithoutTicket() throws Exception {
        Assume.assumeTrue(!System.getProperty("java.vendor").toUpperCase(Locale.ENGLISH).contains("IBM"));
        
        final String cliOutput = CustomCLIExecutor.execute(HOSTNAME_JBOSS_CLI_FILE, WHOAMI, KRB_AND_JAAS_CLI_PARAMS, 
                createKrbEnvironment(KRB5CC_EMPTY_FILE.getAbsolutePath()));
        assertThat("CLI output does not contain outcome success.", cliOutput, containsString("\"outcome\" => \"success\""));
        assertThat("Result of :whoami operation does not contain expected user.", cliOutput, containsString("\"" + JAAS_USER + "\""));
    }
    
    /**
     * Test whether user without Kerberos principal has not granted access to Management CLI. Wrong authentication with Kerberos 
     * is expected to fallback into JAAS authentication in this test.
     */
    @Test
    @InSequence(6)
    public void testAccessWithoutKerberosPrincipal() {
        Assume.assumeTrue(!System.getProperty("java.vendor").toUpperCase(Locale.ENGLISH).contains("IBM"));
        
        String cliOutput = CustomCLIExecutor.execute(HOSTNAME_JBOSS_CLI_FILE, WHOAMI, KRB_AND_JAAS_CLI_PARAMS, createKrbEnvironment(""));
        assertThat("CLI output does not contain outcome success.", cliOutput, containsString("\"outcome\" => \"success\""));
        assertThat("Result of :whoami operation does not contain expected user.", cliOutput, containsString("\"" + JAAS_USER + "\""));
    }
    
    /**
     * Test whether user with valid Kerberos ticket from wrong realm has not granted access to Management CLI. Wrong authentication 
     * with Kerberos is expected to fallback into JAAS authentication in this test.
     */
    @Test
    @InSequence(7)
    public void testAccessWithKerberosTicketFromWrongRealm() {
        Assume.assumeTrue(!System.getProperty("java.vendor").toUpperCase(Locale.ENGLISH).contains("IBM"));
        
        final String cliOutput = CustomCLIExecutor.execute(HOSTNAME_JBOSS_CLI_FILE, WHOAMI, KRB_AND_JAAS_CLI_PARAMS, 
                createKrbEnvironment(KRB5CC_WRONG_REALM_FILE.getAbsolutePath()));
        assertThat("CLI output does not contain outcome success.", cliOutput, containsString("\"outcome\" => \"success\""));
        assertThat("Result of :whoami operation does not contain expected user.", cliOutput, containsString("\"" + JAAS_USER + "\""));
    }
    
    /**
     * Test whether fallback is taken into account when Kerberos server is down. 
     * 
     * THIS TEST ALSO SHUT DOWN KERBEROS SERVER!
     * 
     * @throws Exception 
     */
    @Test
    @InSequence(Integer.MAX_VALUE - 2)
    public void testFallbackWhenKrbServerIsDown() throws Exception {
        Assume.assumeTrue(!System.getProperty("java.vendor").toUpperCase(Locale.ENGLISH).contains("IBM"));
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
        
        final String cliOutput = CustomCLIExecutor.execute(HOSTNAME_JBOSS_CLI_FILE, WHOAMI, KRB_AND_JAAS_CLI_PARAMS, 
                createKrbEnvironment(KRB5CC_FILE.getAbsolutePath()));
        assertThat("CLI output does not contain outcome success.", cliOutput, containsString("\"outcome\" => \"success\""));
        assertThat("Result of :whoami operation does not contain expected user.", cliOutput, containsString("\"" + JAAS_USER + "\""));
    }
    
    
    
    /**
     * Revert the AS configuration and stop the server as the last but one step. Kerberos and directory servers is stopped in 
     * testFallbackWhenKrbServerIsDown() test.
     *
     * @throws Exception
     */
    @Test
    @InSequence(Integer.MAX_VALUE - 1)
    public void closeServer() throws Exception {
        Assume.assumeTrue(!System.getProperty("java.vendor").toUpperCase(Locale.ENGLISH).contains("IBM"));
        
        assertTrue(container.isStarted(DEFAULT_JBOSSAS));
                
        String unsecureNativeInterface = "/core-service=management/management-interface=native-interface:write-attribute"
                + "(name=security-realm,value=" + originalRealm + ")";
        String cliOutput = CustomCLIExecutor.execute(null, unsecureNativeInterface, JAAS_CLI_PARAMS, null);
        assertThat("Revert of native-interface security realm was unsuccessful", cliOutput, containsString("\"outcome\" => \"success\""));
        CustomCLIExecutor.execute(HOSTNAME_JBOSS_CLI_FILE, RELOAD, JAAS_CLI_PARAMS, null);
        CustomCLIExecutor.waitForServerToReload(MAX_RELOAD_TIME, HOSTNAME_JBOSS_CLI_FILE);
        
        assertTrue(container.isStarted(DEFAULT_JBOSSAS));
        
        initCLI();
        final boolean batchResult = runBatch(REMOVE_BATCH_CLI_FILE);
        closeCLI();
        container.stop(DEFAULT_JBOSSAS);

        FileUtils.deleteQuietly(WORK_DIR);

        assertTrue("Reverting server configuration failed", batchResult);
    }
    
    private static List<String> createJaasParams() {
        List<String> params = new ArrayList<String>();
        params.add("--timeout=" + CLI_TIMEOUT);
        params.add("--user=" + JAAS_USER);
        params.add("--password=" + JAAS_PASSWORD);
        return params;
    }
    
    private static List<String> createKrbAndJaasParams() {
        List<String> params = createJaasParams();
        params.add("-Djavax.security.auth.useSubjectCredsOnly=false");
        params.add("-Djava.security.krb5.conf=" + escapePath(Krb5ConfServerSetupTask.getKrb5ConfFullPath()));        
        params.add("-Dsun.security.krb5.debug=true");
        return params;
    }
    
    private Map<String,String> createKrbEnvironment(String cachePath) {
        final Map<String, String> environment = new HashMap<String, String>();
        environment.put("KRB5CCNAME", "FILE:"+escapePath(cachePath));
        environment.put("KRB5_CONFIG", Krb5ConfServerSetupTask.getKrb5ConfFullPath());
        return environment;
    }
    
    static class DirectoryServerSetupTask extends AbstractDirectoryServerSetupTask {

        @Override
        protected InputStream getLdifInputStream() {
            return KerberosHttpInterfaceTestCase.class.getResourceAsStream(KerberosInCLITestCase.class.getSimpleName() + ".ldif");
        }
        
    }
    
    static class Directory2ServerSetupTask extends AbstractDirectory2ServerSetupTask {

        @Override
        protected InputStream getLdifInputStream() {
            return KerberosHttpInterfaceTestCase.class.getResourceAsStream(KerberosInCLITestCase.class.getSimpleName() + "2.ldif");
        }
        
    }
    
}
