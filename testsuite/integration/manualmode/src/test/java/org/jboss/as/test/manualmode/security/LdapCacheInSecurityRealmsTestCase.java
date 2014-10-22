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

package org.jboss.as.test.manualmode.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import junit.framework.AssertionFailedError;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.AnnotationUtils;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.ldap.LdapServer;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.integration.security.common.ManagedCreateLdapServer;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.vfs.VFSUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for LDAP caching in security realms. It checks LDAP caching in following parts of security-realm configuration:
 * <ul>
 * <li>authentication=ldap</li>
 * <li>authorization=ldap/group-search=group-to-principal</li>
 * <li>authorization=ldap/group-search=principal-to-group</li>
 * <li>authorization=ldap/username-to-dn=username-filter</li>
 * <li>authorization=ldap/username-to-dn=advanced-filter</li>
 * </ul>
 *
 * <p>
 * The AS configuration for this testcase is in a single CLI script (ldap-cache-batch.cli) which runs in batch mode.
 * </p>
 *
 * Timeout scenarios are only covered for authentication part of this testcases, because they slow down the testsuite run.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@RunAsClient
public class LdapCacheInSecurityRealmsTestCase extends AbstractCliTestBase {

    private static final String MANAGEMENT_OP_URL = "http://" + TestSuiteEnvironment.getServerAddress()
            + ":9990/management?operation=attribute&name=server-state";
    private static final String MANAGEMENT_OP_EXPECTED_RESULT = "\"running\"";

    private static final String DEFAULT_JBOSSAS = "default-jbossas";

    private static final String TEST_NAME = LdapCacheInSecurityRealmsTestCase.class.getSimpleName();

    private static final String BATCH_CLI_FILENAME = "ldap-cache-batch.cli";
    private static final String REMOVE_BATCH_CLI_FILENAME = "ldap-cache-remove-batch.cli";
    private static final String ROLES_FILENAME = TEST_NAME + "-roles.properties";
    private static final String USERS_FILENAME = TEST_NAME + "-users.properties";
    private static final String LDAP_USERS_FILENAME = TEST_NAME + "-ldapusers.properties";

    private static final File WORK_DIR = new File("ldap-cache-" + System.currentTimeMillis());
    private static final File BATCH_CLI_FILE = new File(WORK_DIR, BATCH_CLI_FILENAME);
    private static final File REMOVE_BATCH_CLI_FILE = new File(WORK_DIR, REMOVE_BATCH_CLI_FILENAME);
    private static final File ROLES_FILE = new File(WORK_DIR, ROLES_FILENAME);
    private static final File USERS_FILE = new File(WORK_DIR, USERS_FILENAME);
    private static final File LDAP_USERS_FILE = new File(WORK_DIR, LDAP_USERS_FILENAME);

    private static final int LDAP_PORT = 10389;

    private static final String SECONDARY_TEST_ADDRESS = TestSuiteEnvironment.getSecondaryTestAddress(false);

    @ArquillianResource
    private static ContainerController container;

    private static final LDAPServerSetupTask ldapSetup = new LDAPServerSetupTask();

    /**
     * Tests authentication=ldap scenario with cache type=by-search-time.
     *
     * @throws Exception
     */
    @Test
    public void testAuthnBySearchTime() throws Exception {
        setHttpInterfaceRealm("ldap-cache-authn-by-search-time-3-1");
        LdapSearchCounterInterceptor.resetCounter();
        // first
        doManagementOperation(true);
        doManagementOperation(false);
        // let it time-out
        Thread.sleep(4 * 1000);
        doManagementOperation(true);
        // another user
        doManagementOperation(true, "jduke2");
        doManagementOperation(false, "jduke2");
        // original user - cache size is 1 - i.e. should be re-readed
        doManagementOperation(true, "jduke");

        // failures not cached
        doManagementOperationWrongUser(true);
        doManagementOperationWrongUser(true);

        printCounterInfo();
    }

    /**
     * Tests authentication=ldap scenario with eviction-time=0 (i.e. unlimited).
     *
     * @throws Exception
     */
    @Test
    public void testAuthnZeroTimeout() throws Exception {
        setHttpInterfaceRealm("ldap-cache-authn-by-search-time-0-3");
        LdapSearchCounterInterceptor.resetCounter();
        doManagementOperation(true);
        doManagementOperation(false);
        doManagementOperation(true, "jduke2");
        doManagementOperation(false);
        doManagementOperation(false, "jduke2");
        doManagementOperation(true, "jduke3");
        doManagementOperation(true, "jduke4");
        doManagementOperation(false, "jduke2");
        doManagementOperation(true);

        printCounterInfo();
    }

    /**
     * Tests if 'cache-failures' flag works for ldap authentication.
     *
     * @throws Exception
     */
    @Test
    public void testAuthnZeroTimeoutWithFailures() throws Exception {
        setHttpInterfaceRealm("ldap-cache-authn-by-access-time-0-2-failures");
        LdapSearchCounterInterceptor.resetCounter();
        doManagementOperation(true);
        doManagementOperationWrongUser(true);
        doManagementOperationWrongUser(false);
        doManagementOperation(false);
        doManagementOperation(true, "jduke2");
        doManagementOperationWrongUser(true);

        printCounterInfo();
    }

    /**
     * Tests if caching works when negative values are given to eviction-time and max-cache-size parameters. Negative values
     * mean unlimited.
     *
     * <p>
     * This test also checks statistics and management operations for the cache (manual flushing, etc.).
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testAuthnNegativeInput() throws Exception {
        setHttpInterfaceRealm("ldap-cache-authn-by-search-time-neg-neg");
        LdapSearchCounterInterceptor.resetCounter();

        doManagementOperation(true, "jduke0");
        doManagementOperation(true, "jduke1");
        doManagementOperation(true, "jduke2");
        doManagementOperation(true, "jduke3");
        doManagementOperation(true, "jduke4");
        doManagementOperation(true, "jduke5");
        doManagementOperation(false, "jduke0");
        doManagementOperation(false, "jduke1");
        doManagementOperation(false, "jduke2");
        doManagementOperation(false, "jduke3");
        doManagementOperation(false, "jduke4");
        doManagementOperation(false, "jduke5");

        CLIOpResult result = null;
        Map<String, Object> resultMap = null;

        final String TRUE = "true";
        final String FALSE = "false";

        cli.sendLine("/core-service=management/security-realm=ldap-cache-authn-by-search-time-neg-neg/authentication=ldap/cache=by-search-time:read-resource(include-runtime=true)");
        result = cli.readAllAsOpResult();
        assertTrue("Reading cache data using management API failed", result.isIsOutcomeSuccess());
        resultMap = result.getResultAsMap();
        assertNotNull("Result Map was expected.", resultMap);
        assertEquals("Unexpected LDAP cache size.", "6", resultMap.get("cache-size"));

        cli.sendLine("/core-service=management/security-realm=ldap-cache-authn-by-search-time-neg-neg/authentication=ldap/cache=by-search-time:contains(name=jduke0)");
        result = cli.readAllAsOpResult();
        assertTrue("Reading cache data using management API failed", result.isIsOutcomeSuccess());
        assertEquals("jduke0 user was expected in the cache", TRUE, result.getResult());

        cli.sendLine("/core-service=management/security-realm=ldap-cache-authn-by-search-time-neg-neg/authentication=ldap/cache=by-search-time:contains(name=jduke)");
        result = cli.readAllAsOpResult();
        assertTrue("Reading cache data using management API failed", result.isIsOutcomeSuccess());
        assertEquals("jduke user was not expected in the cache", FALSE, result.getResult());

        cli.sendLine("/core-service=management/security-realm=ldap-cache-authn-by-search-time-neg-neg/authentication=ldap/cache=by-search-time:flush-cache(name=jduke0)");
        result = cli.readAllAsOpResult();
        assertTrue("Reading cache data using management API failed", result.isIsOutcomeSuccess());

        cli.sendLine("/core-service=management/security-realm=ldap-cache-authn-by-search-time-neg-neg/authentication=ldap/cache=by-search-time:contains(name=jduke0)");
        result = cli.readAllAsOpResult();
        assertTrue("Reading cache data using management API failed", result.isIsOutcomeSuccess());
        assertEquals("jduke0 user was not expected in the cache after the flush", FALSE, result.getResult());

        cli.sendLine("/core-service=management/security-realm=ldap-cache-authn-by-search-time-neg-neg/authentication=ldap/cache=by-search-time:read-resource(include-runtime=true)");
        result = cli.readAllAsOpResult();
        assertTrue("Reading cache data using management API failed", result.isIsOutcomeSuccess());
        resultMap = result.getResultAsMap();
        assertNotNull("Result Map was expected.", resultMap);
        assertEquals("Unexpected LDAP cache size after single user flush.", "5", resultMap.get("cache-size"));

        cli.sendLine("/core-service=management/security-realm=ldap-cache-authn-by-search-time-neg-neg/authentication=ldap/cache=by-search-time:flush-cache()");
        result = cli.readAllAsOpResult();
        assertTrue("Reading cache data using management API failed", result.isIsOutcomeSuccess());

        cli.sendLine("/core-service=management/security-realm=ldap-cache-authn-by-search-time-neg-neg/authentication=ldap/cache=by-search-time:read-resource(include-runtime=true)");
        result = cli.readAllAsOpResult();
        assertTrue("Reading cache data using management API failed", result.isIsOutcomeSuccess());
        resultMap = result.getResultAsMap();
        assertNotNull("Result Map was expected.", resultMap);
        assertEquals("Unexpected LDAP cache size after all users flush.", "0", resultMap.get("cache-size"));

        printCounterInfo();
    }

    /**
     * Tests authentication=ldap scenario with cache type=by-access-time.
     *
     * @throws Exception
     */
    @Test
    public void testAuthnByAccessTime() throws Exception {
        setHttpInterfaceRealm("ldap-cache-authn-by-access-time-3-2");
        LdapSearchCounterInterceptor.resetCounter();
        doManagementOperation(true);
        doManagementOperation(true, "jduke2");
        Thread.sleep(1000);
        doManagementOperation(false);
        Thread.sleep(1000);
        doManagementOperation(false);
        Thread.sleep(1000);
        doManagementOperation(false);
        Thread.sleep(1000);
        doManagementOperation(false);
        doManagementOperation(true, "jduke2");
        Thread.sleep(4 * 1000);
        doManagementOperation(true);
        doManagementOperation(true, "jduke2");
        doManagementOperation(true, "jduke3");
        doManagementOperation(true);

        printCounterInfo();
    }

    /**
     * Tests caching for authorization=ldap/group-search=group-to-principal part of realm configuration.
     *
     * @throws Exception
     */
    @Test
    @Ignore("bz-1150024 HeaderParser fails to handle delimiters in DigestAuthenticator")
    public void testGroupToPrincipal() throws Exception {
        setHttpInterfaceRealm("ldap-cache-authz-group-to-principal-by-search-time-3-1");
        LdapSearchCounterInterceptor.resetCounter();
        doManagementOperation(true, "uid=jduke,ou=Users,dc=jboss,dc=org");
        doManagementOperation(true, "uid=jduke2,ou=Users,dc=jboss,dc=org");
        doManagementOperation(false, "uid=jduke2,ou=Users,dc=jboss,dc=org");
        doManagementOperation(true, "uid=jduke,ou=Users,dc=jboss,dc=org");

        printCounterInfo();
    }

    /**
     * Tests caching for authorization=ldap/group-search=principal-to-group part of realm configuration.
     *
     * @throws Exception
     */
    @Test
    @Ignore("bz-1150024 HeaderParser fails to handle delimiters in DigestAuthenticator")
    public void testPrincipalToGroup() throws Exception {
        setHttpInterfaceRealm("ldap-cache-authz-principal-to-group-by-access-time-3-2");
        LdapSearchCounterInterceptor.resetCounter();
        doManagementOperation(true, "uid=jduke,ou=Users,dc=jboss,dc=org");
        doManagementOperation(true, "uid=jduke2,ou=Users,dc=jboss,dc=org");
        doManagementOperation(false, "uid=jduke2,ou=Users,dc=jboss,dc=org");
        doManagementOperation(false, "uid=jduke,ou=Users,dc=jboss,dc=org");
        doManagementOperation(true, "uid=jduke3,ou=Users,dc=jboss,dc=org");
        doManagementOperation(false, "uid=jduke3,ou=Users,dc=jboss,dc=org");
        doManagementOperation(true, "uid=jduke2,ou=Users,dc=jboss,dc=org");

        printCounterInfo();
    }

    /**
     * Tests caching for authorization=ldap/username-to-dn=username-filter part of relm configuration.
     *
     * @throws Exception
     */
    @Test
    public void testUsernameToDnFilter() throws Exception {
        setHttpInterfaceRealm("ldap-cache-username-to-dn-filter-by-search-time-3-2");
        LdapSearchCounterInterceptor.resetCounter();
        doManagementOperation(true, "jduke");
        doManagementOperation(false, "jduke");
        doManagementOperation(true, "jduke2");
        doManagementOperation(false, "jduke2");
        doManagementOperation(false, "jduke");
        doManagementOperation(true, "jduke3");
        doManagementOperation(true, "jduke");

        printCounterInfo();
    }

    /**
     * Tests caching for authorization=ldap/username-to-dn=advanced-filter part of relm configuration.
     *
     * @throws Exception
     */
    @Test
    public void testUsernameToDnAdvanced() throws Exception {
        setHttpInterfaceRealm("ldap-cache-username-to-dn-advanced-by-access-time-3-1");
        LdapSearchCounterInterceptor.resetCounter();
        doManagementOperation(true, "jduke");
        doManagementOperation(false, "jduke");
        doManagementOperation(true, "jduke2");
        doManagementOperation(false, "jduke2");
        doManagementOperation(true, "jduke");

        printCounterInfo();
    }

    /**
     * Failover scenario which stops LDAP server after the first cached authentication and then waits for the cache timeout and
     * tries authenticate afterwards (failure-expected). Then we start the LDAP server again and test if authentication works.
     *
     * @throws Exception
     */
    @Test
    public void testFailoverAuthn() throws Exception {
        setHttpInterfaceRealm("ldap-cache-authn-by-access-time-3-2");
        LdapSearchCounterInterceptor.resetCounter();
        doManagementOperation(true);
        ldapSetup.stopLdapServer();
        try {
            doManagementOperation(false);
            Thread.sleep(4 * 1000);
            doManagementOperation(false, "jduke", HttpServletResponse.SC_UNAUTHORIZED);
        } finally {
            ldapSetup.startLdapServer();
        }
        doManagementOperation(true);
        doManagementOperation(false);

        printCounterInfo();
    }

    /**
     * Test default behavior of LDAP security realm - i.e. caching is expected to be disabled.
     *
     * @throws Exception
     */
    @Test
    public void testNoCache() throws Exception {
        setHttpInterfaceRealm("ldap-no-cache");
        LdapSearchCounterInterceptor.resetCounter();
        doManagementOperation(true);
        final long firstCallCounter = LdapSearchCounterInterceptor.getCounter();
        doManagementOperation(true);
        assertEquals("Unexpected count of LDAP searches", 2 * firstCallCounter, LdapSearchCounterInterceptor.getCounter());
        doManagementOperation(true);
        assertEquals("Unexpected count of LDAP searches", 3 * firstCallCounter, LdapSearchCounterInterceptor.getCounter());

        printCounterInfo();
    }

    /**
     * This test is not related to LDAP caching. It's a regression test for header parsing in Digest authenticator.
     *
     * @throws Exception
     */
    @Test
    @Ignore("bz-1150024 HeaderParser fails to handle delimiters in DigestAuthenticator")
    public void testDelimitersInUsername() throws Exception {
        setHttpInterfaceRealm("delimiters-test");
        LdapSearchCounterInterceptor.resetCounter();
        doManagementOperation(false, "anil");
        doManagementOperation(false, "backslash\\");
        doManagementOperation(false, "double\"qoute");
        doManagementOperation(false, "ldap=dn,like=username,test=true");

        printCounterInfo();
    }

    /**
     * Logs the LDAP search counter for test method.
     */
    private void printCounterInfo() {
        StackTraceElement caller = new Exception().getStackTrace()[1];
        System.out.println("Count of LDAP searches in the method " + caller.getMethodName() + ": "
                + LdapSearchCounterInterceptor.getCounter());
    }

    // Server configuration part of the TestCase **********************************************

    /**
     * Configure the AS and LDAP as the first step in this testcase.
     *
     * @throws Exception
     */
    @Test
    @InSequence(Integer.MIN_VALUE)
    public void initServer() throws Exception {
        ldapSetup.startDirectoryServer();

        container.start(DEFAULT_JBOSSAS);

        WORK_DIR.mkdirs();

        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream(ROLES_FILENAME), ROLES_FILE);
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream(USERS_FILENAME), USERS_FILE);
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream(LDAP_USERS_FILENAME), LDAP_USERS_FILE);

        final Map<String, String> map = new HashMap<String, String>();
        map.put("rolesProperties", escapePath(ROLES_FILE.getAbsolutePath()));
        map.put("usersProperties", escapePath(USERS_FILE.getAbsolutePath()));
        map.put("ldapUsersProperties", escapePath(LDAP_USERS_FILE.getAbsolutePath()));
        map.put("ldapHost", SECONDARY_TEST_ADDRESS);
        FileUtils.write(BATCH_CLI_FILE,
                StrSubstitutor.replace(IOUtils.toString(getClass().getResourceAsStream(BATCH_CLI_FILENAME), "UTF-8"), map),
                "UTF-8");
        FileUtils.write(REMOVE_BATCH_CLI_FILE, StrSubstitutor.replace(
                IOUtils.toString(getClass().getResourceAsStream(REMOVE_BATCH_CLI_FILENAME), "UTF-8"), map), "UTF-8");

        initCLI();
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
     * Revert the AS configuration and stop the server as the last but one step.
     *
     * @throws Exception
     */
    @Test
    @InSequence(Integer.MAX_VALUE - 1)
    public void closeServer() throws Exception {
        assertTrue(container.isStarted(DEFAULT_JBOSSAS));
        setHttpInterfaceRealm("ManagementRealm");

        final boolean batchResult = runBatch(REMOVE_BATCH_CLI_FILE);
        closeCLI();
        container.stop(DEFAULT_JBOSSAS);

        FileUtils.deleteQuietly(WORK_DIR);

        assertTrue("Reverting server configuration failed", batchResult);
    }

    /**
     * Escapes backslashes in the file path.
     *
     * @param path
     * @return
     */
    private static String escapePath(String path) {
        return path.replace("\\", "\\\\");
    }

    /**
     * Stop the LDAP as a last step in this testcase.
     *
     * @throws Exception
     */
    @Test
    @InSequence(Integer.MAX_VALUE)
    public void stopLdap() throws Exception {
        ldapSetup.shutdownDirectoryServer();
    }

    /**
     * Calls a management operation user "jduke" using HTTP interface and tests if LDAP search was done during the operation.
     * Expected response status code is 200 (SC_OK).
     *
     * @param isLdapSearchExpected
     * @throws MalformedURLException
     * @throws IOException
     * @throws URISyntaxException
     */
    private static void doManagementOperation(boolean isLdapSearchExpected) throws MalformedURLException, IOException,
            URISyntaxException {
        doManagementOperation(isLdapSearchExpected, "jduke");
    }

    /**
     * Calls a management operation with given user using HTTP interface and tests if LDAP search was done during the operation.
     * Expected response status code is 200 (SC_OK).
     *
     * @param isLdapSearchExpected
     * @param userName
     * @throws MalformedURLException
     * @throws IOException
     * @throws URISyntaxException
     */
    private static void doManagementOperation(boolean isLdapSearchExpected, final String userName)
            throws MalformedURLException, IOException, URISyntaxException {
        doManagementOperation(isLdapSearchExpected, userName, HttpServletResponse.SC_OK);
    }

    /**
     * Calls a management operation using HTTP interface and tests if expected response comes and if LDAP search was done during
     * the operation.
     *
     * @param isLdapSearchExpected
     * @param userName user to authenticate (password is always "theduke")
     * @param expectedStatus expected HTTP response status code
     * @throws MalformedURLException
     * @throws IOException
     * @throws URISyntaxException
     */
    private static void doManagementOperation(boolean isLdapSearchExpected, final String userName, int expectedStatus)
            throws MalformedURLException, IOException, URISyntaxException {
        final long counter = LdapSearchCounterInterceptor.getCounter();
        String response = Utils.makeCallWithBasicAuthn(new URL(MANAGEMENT_OP_URL), userName, "theduke", expectedStatus);
        if (expectedStatus == HttpServletResponse.SC_OK) {
            assertEquals("Unexpected response from HTTP management interface", MANAGEMENT_OP_EXPECTED_RESULT, response);
        }
        final long actualCounter = LdapSearchCounterInterceptor.getCounter();
        if (isLdapSearchExpected) {
            if (counter >= actualCounter) {
                throw new AssertionFailedError(
                        "Increasing LDAP search counter value was expected. Counter values were [ original=" + counter
                                + ", actual=" + actualCounter + "]");
            }
        } else {
            assertEquals("No additional LDAP search was expected.", counter, actualCounter);
        }
    }

    /**
     * Tests management request with wrong username and checks if LDAP search has occurred.
     *
     * @param isLdapSearchExpected
     * @throws MalformedURLException
     * @throws IOException
     * @throws URISyntaxException
     */
    private static void doManagementOperationWrongUser(boolean isLdapSearchExpected) throws MalformedURLException, IOException,
            URISyntaxException {
        doManagementOperation(isLdapSearchExpected, "test", HttpServletResponse.SC_UNAUTHORIZED);
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
     * Sets security-realm attribute for HTTP management interface.
     *
     * @param realmName realm name to set
     * @throws IOException
     */
    private static void setHttpInterfaceRealm(String realmName) throws IOException {
        cli.sendLine(
                "/core-service=management/management-interface=http-interface:write-attribute(name=security-realm, value=\""
                        + realmName + "\")", false);
        assertTrue("Setting http-interface's attribute security-realm failed", cli.readAllAsOpResult().isIsOutcomeSuccess());
    }

    /**
     * A server setup task which configures and starts LDAP server. It enables additional ApacheDS interceptor to count LDAP
     * search requests.
     */
    @CreateDS(name = "JBossDS", factory = org.jboss.as.test.integration.ldap.InMemoryDirectoryServiceFactory.class,
            partitions = { @CreatePartition(name = "jboss", suffix = "dc=jboss,dc=org") },
            additionalInterceptors = { LdapSearchCounterInterceptor.class })
    @CreateLdapServer(transports = { @CreateTransport(protocol = "LDAP", port = LDAP_PORT, address = "0.0.0.0") })
    static class LDAPServerSetupTask {

        private DirectoryService directoryService;
        private LdapServer ldapServer;

        /**
         * Creates directory services, starts LDAP server.
         */
        public void startDirectoryServer() throws Exception {
            directoryService = DSAnnotationProcessor.getDirectoryService();
            final SchemaManager schemaManager = directoryService.getSchemaManager();
            final Class<LdapCacheInSecurityRealmsTestCase> clazz = LdapCacheInSecurityRealmsTestCase.class;
            LdifReader ldifReader = new LdifReader(clazz.getResourceAsStream(clazz.getSimpleName() + ".ldif"));
            try {
                for (LdifEntry ldifEntry : ldifReader) {
                    directoryService.getAdminSession().add(new DefaultEntry(schemaManager, ldifEntry.getEntry()));
                }
            } finally {
                VFSUtils.safeClose(ldifReader);
            }
            final ManagedCreateLdapServer createLdapServer = new ManagedCreateLdapServer(
                    (CreateLdapServer) AnnotationUtils.getInstance(CreateLdapServer.class));
            Utils.fixApacheDSTransportAddress(createLdapServer, StringUtils.strip(SECONDARY_TEST_ADDRESS));
            ldapServer = ServerAnnotationProcessor.instantiateLdapServer(createLdapServer, directoryService);
            startLdapServer();
        }

        /**
         * Stops the LDAP server, directory service and removes the working files of ApacheDS.
         */
        public void shutdownDirectoryServer() throws Exception {
            stopLdapServer();
            directoryService.shutdown();
            FileUtils.deleteDirectory(directoryService.getInstanceLayout().getInstanceDirectory());
        }

        /**
         * Starts LDAP server instance.
         *
         * @throws Exception
         */
        public void startLdapServer() throws Exception {
            ldapServer.start();
        }

        /**
         * Stops LDAP server instance.
         *
         * @throws Exception
         */
        public void stopLdapServer() {
            ldapServer.stop();
        }
    }

}
