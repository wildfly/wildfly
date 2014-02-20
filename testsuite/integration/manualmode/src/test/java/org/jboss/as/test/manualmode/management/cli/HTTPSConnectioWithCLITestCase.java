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

package org.jboss.as.test.manualmode.management.cli;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.util.CustomCLIExecutor;
import org.jboss.as.test.integration.security.common.AbstractSecurityRealmsServerSetupTask;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.realm.Authentication;
import org.jboss.as.test.integration.security.common.config.realm.RealmKeystore;
import org.jboss.as.test.integration.security.common.config.realm.SecurityRealm;
import org.jboss.as.test.integration.security.common.config.realm.ServerIdentity;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testing https connection to native management interface with cli console with
 * configured two-way SSL. CLI client uses client truststore with accepted
 * server's certificate and vice-versa. Keystores and truststores have valid
 * certificates until 25 October 2033.
 * 
 * @author Filip Bogyai
 */

@RunWith(Arquillian.class)
@ServerSetup({ HTTPSConnectioWithCLITestCase.ManagementNativeRealmSetup.class, HTTPSConnectioWithCLITestCase.KeystoreFilesSetup.class })
@RunAsClient
public class HTTPSConnectioWithCLITestCase {

    private static Logger LOGGER = Logger.getLogger(HTTPSConnectioWithCLITestCase.class);

    private static final File WORK_DIR = new File("native-if-workdir");
    public static final File SERVER_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_KEYSTORE);
    public static final File SERVER_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_TRUSTSTORE);
    public static final File CLIENT_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.CLIENT_KEYSTORE);
    public static final File CLIENT_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.CLIENT_TRUSTSTORE);
    public static final File UNTRUSTED_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.UNTRUSTED_KEYSTORE);

    private static final String MANAGEMENT_NATIVE_REALM = "ManagementNativeRealm";
    private static final String JBOSS_CLI_FILE = "jboss-cli.xml";
    private static final File TRUSTED_JBOSS_CLI_FILE = new File(WORK_DIR, "trusted-jboss-cli.xml");
    private static final File UNTRUSTED_JBOSS_CLI_FILE = new File(WORK_DIR, "untrusted-jboss-cli.xml");

    private static final String TESTING_OPERATION = "/core-service=management/management-interface=http-interface:read-resource";
    private static final String RELOAD = "reload";
    private static final int MAX_RELOAD_TIME = 30000;

    private static final String CONTAINER = "default-jbossas";
    private static final KeystoreFilesSetup keystoreFilesSetup = new KeystoreFilesSetup();
    private static final ManagementNativeRealmSetup managementNativeRealmSetup = new ManagementNativeRealmSetup();

    @ArquillianResource
    private static ContainerController containerController;

    @Test
    @InSequence(-1)
    public void prepareServer() throws Exception {

        LOGGER.info("*** starting server");
        containerController.start(CONTAINER);
        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        ManagementClient mgmtClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getServerPort());
        keystoreFilesSetup.setup(mgmtClient, CONTAINER);
        managementNativeRealmSetup.setup(mgmtClient, CONTAINER);

        mgmtClient.close();
        // To apply new security realm settings for native interface reload of
        // server is required
        LOGGER.info("*** restart server");
        CustomCLIExecutor.execute(TRUSTED_JBOSS_CLI_FILE, RELOAD);
        CustomCLIExecutor.waitForServerToReload(MAX_RELOAD_TIME, TRUSTED_JBOSS_CLI_FILE);

    }

    /**
     * Testing connection to server native interface with default CLI settings.
     * Client doesn't have servers certificate in truststore and therefore it
     * rejects connection.
     */
    @Test
    @InSequence(1)
    public void testDefaultConfigurationOverNative() throws InterruptedException, IOException {

        String cliOutput = CustomCLIExecutor.execute(null, TESTING_OPERATION);
        assertThat("Untrusted client should not be authenticated.", cliOutput, not(containsString("\"outcome\" => \"success\"")));
    }

    /**
     * Testing connection to server native interface with CLI using wrong
     * certificate. Server doesn't have client certificate in truststore and
     * therefore it rejects connection.
     * 
     */
    @Test
    @InSequence(2)
    public void testUntrustedCertificateOverNative() throws InterruptedException, IOException {

        String cliOutput = CustomCLIExecutor.execute(UNTRUSTED_JBOSS_CLI_FILE, TESTING_OPERATION);
        assertThat("Untrusted client should not be authenticated.", cliOutput, not(containsString("\"outcome\" => \"success\"")));
    }

    /**
     * Testing connection to server native interface with CLI using trusted
     * certificate. Client has server certificate in truststore, and also server
     * has certificate from client, so client can successfully connect.
     * 
     */
    @Test
    @InSequence(3)
    public void testTrustedCertificateOverNative() throws InterruptedException, IOException {

        String cliOutput = CustomCLIExecutor.execute(TRUSTED_JBOSS_CLI_FILE, TESTING_OPERATION);
        assertThat("Trusted client should be authenticated.", cliOutput, containsString("\"outcome\" => \"success\""));
    }

    @Test
    @InSequence(4)
    public void resetConfigurationForNativeInterface() throws Exception {

        LOGGER.info("*** reseting test configuration");
        // change back security realm for native management interface
        String unsecureNativeInterface = "/core-service=management/management-interface=native-interface:write-attribute(name=security-realm,value=ManagementRealm";
        String cliOutput = CustomCLIExecutor.execute(TRUSTED_JBOSS_CLI_FILE, unsecureNativeInterface);
        assertThat("Revert of native-interface security realm was unsuccessful", cliOutput, containsString("\"outcome\" => \"success\""));

        // reload to apply changes
        CustomCLIExecutor.execute(TRUSTED_JBOSS_CLI_FILE, RELOAD);
        CustomCLIExecutor.waitForServerToReload(MAX_RELOAD_TIME, TRUSTED_JBOSS_CLI_FILE);

        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        ManagementClient mgmtClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getServerPort());
        keystoreFilesSetup.tearDown(mgmtClient, CONTAINER);
        managementNativeRealmSetup.tearDown(mgmtClient, CONTAINER);
        mgmtClient.close();

        LOGGER.info("*** stopping container");
        containerController.stop(CONTAINER);

    }

    static class ManagementNativeRealmSetup extends AbstractSecurityRealmsServerSetupTask {

        @Override
        protected SecurityRealm[] getSecurityRealms() throws Exception {
            final ServerIdentity serverIdentity = new ServerIdentity.Builder().ssl(
                    new RealmKeystore.Builder().keystorePassword(SecurityTestConstants.KEYSTORE_PASSWORD)
                            .keystorePath(SERVER_KEYSTORE_FILE.getAbsolutePath()).build()).build();
            final Authentication authentication = new Authentication.Builder().truststore(
                    new RealmKeystore.Builder().keystorePassword(SecurityTestConstants.KEYSTORE_PASSWORD)
                            .keystorePath(SERVER_TRUSTSTORE_FILE.getAbsolutePath()).build()).build();
            final SecurityRealm realm = new SecurityRealm.Builder().name(MANAGEMENT_NATIVE_REALM).serverIdentity(serverIdentity)
                    .authentication(authentication).build();
            return new SecurityRealm[] { realm };
        }
    }

    static class KeystoreFilesSetup implements ServerSetupTask {

        public void setup(ManagementClient managementClient, String containerId) throws Exception {

            // create key and trust stores with imported certificates from
            // opposing sides
            FileUtils.deleteDirectory(WORK_DIR);
            WORK_DIR.mkdirs();
            Utils.createKeyMaterial(WORK_DIR);

            // create jboss-cli.xml files with valid/invalid keystore
            // certificates
            FileUtils.write(TRUSTED_JBOSS_CLI_FILE, Utils.propertiesReplacer(JBOSS_CLI_FILE, CLIENT_KEYSTORE_FILE, CLIENT_TRUSTSTORE_FILE,
                    SecurityTestConstants.KEYSTORE_PASSWORD));
            FileUtils.write(UNTRUSTED_JBOSS_CLI_FILE, Utils.propertiesReplacer(JBOSS_CLI_FILE, UNTRUSTED_KEYSTORE_FILE,
                    CLIENT_TRUSTSTORE_FILE, SecurityTestConstants.KEYSTORE_PASSWORD));

            final ModelControllerClient client = managementClient.getControllerClient();

            // secure native interface
            ModelNode operation = createOpNode("core-service=management/management-interface=native-interface",
                    ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
            operation.get(NAME).set("security-realm");
            operation.get(VALUE).set(MANAGEMENT_NATIVE_REALM);
            Utils.applyUpdate(operation, client);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {

            FileUtils.deleteDirectory(WORK_DIR);

        }
    }
}
