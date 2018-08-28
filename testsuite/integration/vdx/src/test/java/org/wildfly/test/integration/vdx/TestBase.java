/*
 * Copyright 2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  *
 * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.wildfly.test.integration.vdx;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.wildfly.test.integration.vdx.utils.server.ServerBase;
import org.wildfly.test.integration.vdx.utils.server.Server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Do not inherit from this class as it's common for standalone and domain tests! For standalone tests inherit from
 */
public class TestBase {

    public static final String STANDALONE_ARQUILLIAN_CONTAINER = "jboss";
    public static final String DOMAIN_ARQUILLIAN_CONTAINER = "jboss-domain";
    static final Logger log = Logger.getLogger(TestBase.class);

    @ArquillianResource private ContainerController controller;

    @Rule public TestName testName = new TestName();

    /*
    * Path to root directory where server.log and xml configuration for each test is archived
    */
    private Path testArchiveDirectory;

    public Server container() {
        return ServerBase.getOrCreateServer(controller);
    }

    @Before public void setUp() throws Exception {
        log.debug("----------------------------------------- Start " +
                this.getClass().getSimpleName() + " - " + testName.getMethodName() + " -----------------------------------------");

        testArchiveDirectory = Paths.get("target", "test-output", this.getClass().getSimpleName(), testName.getMethodName());
        createAndSetTestArchiveDirectoryToContainer(testArchiveDirectory);
    }

    @After public void tearDown() throws Exception {
        log.debug("----------------------------------------- Stop " +
                this.getClass().getSimpleName() + " - " + testName.getMethodName() + " -----------------------------------------");
        archiveServerLogAndDeleteIt(testArchiveDirectory);
    }

    protected static void assertContains(String errorMessages, String expectedMessage) {
        assertTrue("log doesn't contain '" + expectedMessage + "'", errorMessages.contains(expectedMessage));
    }

    protected static void assertDoesNotContain(String errorMessages, String expectedMessage) {
        assertFalse("log contains '" + expectedMessage + "'", errorMessages.contains(expectedMessage));
    }

    private void archiveServerLogAndDeleteIt(Path pathToArchiveDirectory) throws Exception {

        // if no log then return
        if (Files.notExists(container().getServerLogPath())) {
            return;
        }

        // copy server.log files for standalone or host-controller.log for domain
        Files.copy(container().getServerLogPath(), pathToArchiveDirectory.resolve(container().getServerLogPath().getFileName()), StandardCopyOption.REPLACE_EXISTING);
        Files.delete(container().getServerLogPath());
    }

    private void createAndSetTestArchiveDirectoryToContainer(Path testArchiveDirectory) throws Exception {
        // create directory with name of the test in target directory
        if (Files.notExists(testArchiveDirectory)) {
            Files.createDirectories(testArchiveDirectory);
        }
        container().setTestArchiveDirectory(testArchiveDirectory);
    }
}
