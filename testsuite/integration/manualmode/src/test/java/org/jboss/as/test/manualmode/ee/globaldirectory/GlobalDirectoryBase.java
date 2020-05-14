/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.manualmode.ee.globaldirectory;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;

/**
 * Base class for global directory tests. This class contains mostly util methods that are used in tests.
 *
 * @author Vratislav Marek (vmarek@redhat.com)
 * @author Tomas Terem (tterem@redhat.com)
 **/
public class GlobalDirectoryBase extends AbstractCliTestBase {

    protected static final String SUBSYSTEM_EE = "ee";
    protected static final Path GLOBAL_DIRECTORY_PATH = Paths.get(TestSuiteEnvironment.getTmpDir(), "global-directory");
    protected static final File GLOBAL_DIRECTORY_FILE = GLOBAL_DIRECTORY_PATH.toFile();
    protected static final String GLOBAL_DIRECTORY_NAME = "global-directory";
    protected static final Path SECOND_GLOBAL_DIRECTORY_PATH = Paths.get(TestSuiteEnvironment.getTmpDir(),"global-directory-2");
    protected static final String SECOND_GLOBAL_DIRECTORY_NAME = "global-directory-2";

    protected static final String DUPLICATE_ERROR_GLOBAL_DIRECTORY_CODE = "WFLYEE0123";

    protected static final File TEMP_DIR = new File(TestSuiteEnvironment.getTmpDir(), "jars");

    protected static final String CONTAINER = "default-jbossas";
    protected static final String DEPLOYMENT = "deployment";
    protected static final String DEPLOYMENT2 = "deployment2";
    protected static final String DEPLOYMENT3 = "deployment3";

    protected static final Logger LOGGER = Logger.getLogger(GlobalDirectoryBase.class);

    protected ClientHolder clientHolder;
    protected Client client = ClientBuilder.newClient();

    @ArquillianResource
    protected static ContainerController containerController;

    @ArquillianResource
    protected static Deployer deployer;

    protected void createGlobalDirectoryFolder() {
        if (Files.notExists(GLOBAL_DIRECTORY_PATH)) {
            GLOBAL_DIRECTORY_PATH.toFile().mkdirs();
        }
        if (Files.notExists(SECOND_GLOBAL_DIRECTORY_PATH)) {
            SECOND_GLOBAL_DIRECTORY_PATH.toFile().mkdirs();
        }
    }

    protected static void createLibrary(String name, Class library) {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, name + ".jar").addClasses(library);
        if (Files.notExists(Paths.get(TEMP_DIR.toString()))) {
            TEMP_DIR.mkdirs();
        }
        jar.as(ZipExporter.class).exportTo(new File(TEMP_DIR, name + ".jar"), true);
    }

    protected static void createTextFile(String name, List<String> content) throws IOException {
        if (Files.notExists(Paths.get(TEMP_DIR.toString()))) {
            TEMP_DIR.mkdirs();
        }
        Path file = new File(TEMP_DIR, name + ".txt").toPath();
        Files.write(file, content, StandardCharsets.UTF_8);
    }

    protected static void createCorruptedLibrary(String name, List<String> content) throws IOException {
        if (Files.notExists(Paths.get(TEMP_DIR.toString()))) {
            TEMP_DIR.mkdirs();
        }
        Path file = new File(TEMP_DIR, name + ".jar").toPath();
        Files.write(file, content, StandardCharsets.UTF_8);
    }

    protected void copyTextFileToGlobalDirectory(String name) throws IOException {
        if (Files.notExists(GLOBAL_DIRECTORY_PATH)) {
            GLOBAL_DIRECTORY_PATH.toFile().mkdirs();
        }
        Path filePath = new File(TEMP_DIR, name + ".txt").toPath();
        Files.copy(filePath, new File(GLOBAL_DIRECTORY_FILE, name + ".txt").toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    protected void copyLibraryToGlobalDirectory(String name) throws IOException {
        if (Files.notExists(GLOBAL_DIRECTORY_PATH)) {
            GLOBAL_DIRECTORY_PATH.toFile().mkdirs();
        }
        Path jarPath = new File(TEMP_DIR, name + ".jar").toPath();
        Files.copy(jarPath, new File(GLOBAL_DIRECTORY_FILE, name + ".jar").toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    protected void copyLibraryToDirectory(String name, String path) throws IOException {
        if (Files.notExists(Paths.get(path))) {
            Paths.get(path).toFile().mkdirs();
        }
        Path jarPath = new File(TEMP_DIR, name + ".jar").toPath();
        Files.copy(jarPath, new File(path, name + ".jar").toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    protected void logContains(String expectedMessage) throws IOException {
        String serverLog = String.join("\n", Files.readAllLines(getServerLogFile().toPath()));
        assertTrue("Log doesn't contain '" + expectedMessage + "'", serverLog.contains(expectedMessage));
    }

    @Before
    public void before() throws IOException {
        if (!containerController.isStarted(CONTAINER)) {
            containerController.start(CONTAINER);
        }
        createGlobalDirectoryFolder();
        connect();
    }

    @After
    public void after() {
        if (containerController.isStarted(CONTAINER)) {
            containerController.stop(CONTAINER);
        }
        client.close();
        disconnect();
    }

    protected void connect() {
        if (clientHolder == null) {
            clientHolder = ClientHolder.init();
        }
    }

    protected void disconnect() {
        clientHolder = null;
    }

    protected void reloadServer() {
        ServerReload.executeReloadAndWaitForCompletion(clientHolder.mgmtClient);
    }

    /**
     * @param expectedJars Expected Jars, the order matter, it will compare order of jars in the log
     */
    protected void checkJarLoadingOrder(String[] expectedJars) throws IOException {
        String[] logs = Files.readAllLines(getServerLogFile().toPath()).toArray(new String[]{});
        int i = 0;
        while (!logs[i].contains(expectedJars[0])) {
            i++;
        }
        for (int j = 0; j < expectedJars.length; j++) {
            assertThat("Jars were not loaded in correct order!", logs[i], containsString(expectedJars[j]));
            i++;
        }
    }

    /**
     * Register global directory
     * Verify the response for success
     *
     * @param name Name of new global directory
     */
    protected ModelNode registerGlobalDirectory(String name) throws IOException {
        return registerGlobalDirectory(name, GLOBAL_DIRECTORY_PATH.toString(), true);
    }

    /**
     * Register global directory
     *
     * @param name          Name of new global directory
     * @param path
     * @param expectSuccess If is true verify the response for success, if false only return operation result
     */
    protected ModelNode registerGlobalDirectory(String name, String path, boolean expectSuccess) throws IOException {
        // /subsystem=ee/global-directory=<<name>>:add(path=<<path>>)
        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, SUBSYSTEM_EE)
                .add(GLOBAL_DIRECTORY_NAME, name)
                .protect();
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(INCLUDE_RUNTIME).set(true);
        operation.get(OP_ADDR).set(address);
        operation.get(PATH).set(path);

        ModelNode response = clientHolder.execute(operation);
        ModelNode outcome = response.get(OUTCOME);
        if (expectSuccess) {
            assertThat("Registration of global directory " + name + " failure!", outcome.asString(), is(SUCCESS));
        }
        return response;
    }

    /**
     * Remove global directory
     *
     * @param name Name of global directory for removing
     */
    protected ModelNode removeGlobalDirectory(String name) throws IOException {
        // /subsystem=ee/global-directory=<<name>>:remove
        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, SUBSYSTEM_EE)
                .add(GLOBAL_DIRECTORY_NAME, name)
                .protect();
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(REMOVE);
        operation.get(INCLUDE_RUNTIME).set(true);
        operation.get(OP_ADDR).set(address);

        ModelNode response = clientHolder.execute(operation);
        ModelNode outcome = response.get(OUTCOME);
        assertThat("Remove of global directory " + name + "  failure!", outcome.asString(), is(SUCCESS));
        return response;
    }

    /**
     * Verify if global directory is registered and contains correct path
     *
     * @param name Name of global directory
     * @param path Expected path for current global directory
     */
    protected ModelNode verifyProperlyRegistered(String name, String path) throws IOException {
        ModelNode response = readGlobalDirectory(name);
        ModelNode outcome = response.get(OUTCOME);
        assertThat("Read resource of global directory " + name + " failure!", outcome.asString(), is(SUCCESS));

        final ModelNode result = response.get(RESULT);
        assertThat("Global directory " + name + " have set wrong path!", result.get(PATH).asString(), is(path));
        return response;
    }

    /**
     * Verify that global directory doesn't exist
     *
     * @param name Name of global directory
     */
    protected ModelNode verifyDoesNotExist(String name) throws IOException {
        ModelNode response = readGlobalDirectory(name);
        ModelNode outcome = response.get(OUTCOME);
        assertThat("Global directory " + name + " still exist!", outcome.asString(), not(SUCCESS));
        return response;
    }

    /**
     * Read resource command for global directory
     *
     * @param name Name of global directory
     */
    private ModelNode readGlobalDirectory(String name) throws IOException {
        // /subsystem=ee/global-directory=<<name>>:read-resource
        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, SUBSYSTEM_EE)
                .add(GLOBAL_DIRECTORY_NAME, name)
                .protect();
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);
        operation.get(OP_ADDR).set(address);

        return clientHolder.execute(operation);
    }

    private static class ClientHolder {

        private final ManagementClient mgmtClient;

        private ClientHolder(ManagementClient mgmtClient) {
            this.mgmtClient = mgmtClient;
        }

        protected static ClientHolder init() {
            final ModelControllerClient clientHolder = TestSuiteEnvironment.getModelControllerClient();
            ManagementClient mgmtClient = new ManagementClient(clientHolder, TestSuiteEnvironment.formatPossibleIpv6Address(TestSuiteEnvironment.getServerAddress()),
                    TestSuiteEnvironment.getServerPort(), "http-remoting");
            return new ClientHolder(mgmtClient);
        }

        /**
         * Execute operation in wildfly
         *
         * @param operation Cli command represent in ModelNode interpretation
         */
        protected ModelNode execute(final ModelNode operation) throws
                IOException {
            return mgmtClient.getControllerClient().execute(operation);
        }

    }

    protected File getServerLogFile() {
        String JBOSS_HOME = System.getProperty("jboss.home", "jboss-as");
        String SERVER_MODE = Boolean.parseBoolean(System.getProperty("domain", "false")) ? "domain" : "standalone";
        return Paths.get(JBOSS_HOME, SERVER_MODE, "log", "server.log").toFile();
    }
}
