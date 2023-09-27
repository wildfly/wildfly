/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.manual.management;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.jboss.dmr.ModelNode;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.wildfly.core.testrunner.ManagementClient;
import org.wildfly.core.testrunner.ServerControl;
import org.wildfly.core.testrunner.ServerController;
import org.wildfly.core.testrunner.WildFlyRunner;

import jakarta.inject.Inject;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.controller.PathAddress;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import org.jboss.as.controller.operations.common.Util;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.wildfly.core.testrunner.Server;

/**
 * Apply CLI script to evolve standalone configurations with MP.
 *
 * @author jdenise
 */
@RunWith(WildFlyRunner.class)
@ServerControl(manual = true)
public class MPScriptTestCase {

    private static final File ROOT = new File(System.getProperty("jboss.home"));
    private static final Path CONFIG_DIR = ROOT.toPath().resolve("standalone").resolve("configuration");
    private static final Path SCRIPT_FILE = ROOT.toPath().resolve("docs").resolve("examples").resolve("enable-microprofile.cli");
    private static final String CONFIG_FILE_PROP = "config";
    private static final String PREFIX = "copy-";

    @Inject
    private ServerController serverController;

    private String currentConfig;

    @Before
    public void check() {
        Assume.assumeTrue(String.format("Configuration file %s not found. Skipping these tests.", SCRIPT_FILE), Files.exists(SCRIPT_FILE));
    }

    @Test
    public void test() throws Exception {
        currentConfig = "standalone.xml";
        setupConfig();
        doTest();
    }

    @Test
    public void testFull() throws Exception {
        currentConfig = "standalone-full.xml";
        setupConfig();
        doTest();
    }

    @Test
    public void testHa() throws Exception {
        currentConfig = "standalone-ha.xml";
        setupConfig();
        doTest();
    }

    @Test
    public void testFullHa() throws Exception {
        currentConfig = "standalone-full-ha.xml";
        setupConfig();
        doTest();
    }

    @After
    public void resetConfig() throws Exception {
        Path copy = CONFIG_DIR.resolve(PREFIX + currentConfig);
        if (Files.exists(copy)) {
            Files.move(copy,
                    CONFIG_DIR.resolve(currentConfig), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void doTest() throws Exception {
        serverController.start(currentConfig, Server.StartMode.NORMAL);
        try {
            ManagementClient client = serverController.getClient();
            ModelNode rr = Util.createEmptyOperation(READ_RESOURCE_OPERATION, PathAddress.EMPTY_ADDRESS);
            rr.get(RECURSIVE).set(true);
            ModelNode result = client.executeForResult(rr);

            //Check subsystems are present and security removed
            Assert.assertTrue(result.has(SUBSYSTEM, "microprofile-fault-tolerance-smallrye"));
            Assert.assertTrue(result.has(SUBSYSTEM, "microprofile-jwt-smallrye"));
            Assert.assertTrue(result.has(SUBSYSTEM, "microprofile-openapi-smallrye"));
            Assert.assertFalse(result.has(SUBSYSTEM, "security"));
        } finally {
            serverController.stop();
        }
    }

    private void setupConfig() throws Exception {
        System.setProperty(CONFIG_FILE_PROP, currentConfig);
        try {
            Files.copy(CONFIG_DIR.resolve(currentConfig), CONFIG_DIR.resolve(PREFIX + currentConfig));
            CommandContext ctx = CommandContextFactory.getInstance().newCommandContext();
            for (String line : Files.readAllLines(SCRIPT_FILE, Charset.forName("UTF-8"))) {
                ctx.handle(line);
            }
        } finally {
            System.clearProperty(CONFIG_FILE_PROP);
        }
    }
}
