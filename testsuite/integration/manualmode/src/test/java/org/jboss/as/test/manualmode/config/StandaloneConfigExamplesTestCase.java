/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.config;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.core.testrunner.ManagementClient;
import org.wildfly.core.testrunner.Server;
import org.wildfly.core.testrunner.ServerControl;
import org.wildfly.core.testrunner.ServerController;
import org.wildfly.core.testrunner.UnsuccessfulOperationException;
import org.wildfly.core.testrunner.WildFlyRunner;


/**
 * Test that the server starts with the provided standalone configurations from
 * the docs/examples/configs directory.
 * These configuration files requires additional system properties and environment
 * variables to be set in order to start the server. This test only verifies that the server
 * starts without errors using these files.
 */
@RunWith(WildFlyRunner.class)
@ServerControl(manual = true)
public class StandaloneConfigExamplesTestCase {
    static String baseDir = System.getProperty("basedir");
    static Path sourceDirectory = Path.of(baseDir, "target", "wildfly", "docs", "examples", "configs");
    static Path targetDirectory = Path.of(baseDir, "target", "wildfly", "standalone", "configuration");
    static List<Path> configUnderTests = new ArrayList<>();
    static Set<String> bannedConfigExamples = new HashSet<>();

    static {
        // Requires configuration of the AWS credentials
        bannedConfigExamples.add("standalone-ec2-full-ha.xml");
        // Requires configuration of the Azure credentials
        bannedConfigExamples.add("standalone-azure-full-ha.xml");
        // Requires a Generic JMS JCA Resource Adapter
        bannedConfigExamples.add("standalone-genericjms.xml");
    }

    @Inject
    ServerController container;

    @BeforeClass
    public static void setUp() throws IOException {

        try (Stream<Path> files = Files.list(sourceDirectory)) {
            configUnderTests.addAll(files.filter(p -> {
                        String fileName = p.getFileName().toString();
                        return fileName.startsWith("standalone") && fileName.endsWith(".xml") && !bannedConfigExamples.contains(fileName);
                    })
                    .collect(Collectors.toUnmodifiableList()));
        }

        for (Path config : configUnderTests) {
            Files.copy(config, targetDirectory.resolve(config.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @AfterClass
    public static void cleanUp() throws IOException {
        for (Path config : configUnderTests) {
            Files.deleteIfExists(targetDirectory.resolve(config.getFileName()));
        }
    }

    @Test
    public void testServerStarts() throws Exception {
        Map<String, String> requiredArgs = getRequiredServerArgs();

        StringBuilder serverArgs = new StringBuilder(System.getProperty("jboss.args"));
        if (serverArgs.length() > 0) {
            serverArgs.append(" ");
        }
        String result = requiredArgs.entrySet().stream()
                .map(entry -> "-D" + entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(" "));
        serverArgs.append(result);

        System.setProperty("jboss.args", serverArgs.toString());

        for (Path config : configUnderTests) {
            String configName = config.getFileName().toString();
            try {
                container.start(configName, Server.StartMode.NORMAL);
                Assert.assertTrue("Server using " + configName + " configuration did not start.", container.isStarted());
                ModelNode bootErrors = readBootErrors(container.getClient());
                Assert.assertTrue("Server using " + configName + " produced the following errors at boot: \n" + bootErrors.asString(), !bootErrors.isDefined() || bootErrors.asInt() == 0);
            } finally {
                container.stop();
            }
        }
    }

    private static Map<String, String> getRequiredServerArgs() {
        Map<String, String> requiredArgs = new HashMap<>();
        // Required by standalone-azure-ha.xml
        requiredArgs.put("jboss.jgroups.azure_ping.storage_account_name", "azaccount");
        requiredArgs.put("jboss.jgroups.azure_ping.storage_access_key", "8n/Z5R0VRxL9GJnH6w8dL2q0Ksl3XHNV0F2kA1uZjq1QWQFnW71F+F3slkVvJk5cWYqO5YfYP3F9KtR1g7G3Kg==");
        requiredArgs.put("jboss.jgroups.azure_ping.container", "azcontainer");
        // Required by standalone-ec2-ha.xml
        requiredArgs.put("jboss.jgroups.aws.s3_ping.region_name", "us-west-2");
        requiredArgs.put("jboss.jgroups.aws.s3_ping.bucket_name", "s3bucket");
        // Required by standalone-gossip-full-ha.xml
        requiredArgs.put("jboss.jgroups.gossip.initial_hosts", "gossip-server[8080]");
        requiredArgs.put("jboss.jgroups.gossip.num_initial_members", "3");

        return requiredArgs;
    }

    private ModelNode readBootErrors(ManagementClient managementClient) throws UnsuccessfulOperationException {
        ModelNode readBootOp = Operations.createOperation("read-boot-errors", PathAddress.pathAddress(CORE_SERVICE, MANAGEMENT).toModelNode());
        return managementClient.executeForResult(readBootOp);
    }
}
