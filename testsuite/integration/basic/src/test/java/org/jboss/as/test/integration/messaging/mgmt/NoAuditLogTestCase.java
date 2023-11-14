/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.messaging.mgmt;

import static org.jboss.shrinkwrap.api.ArchivePaths.create;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This is a simple test that deploys an application then checks the logs for ActiveMQ audit logs.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunAsClient
@RunWith(Arquillian.class)
public class NoAuditLogTestCase {

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static JavaArchive deploy() {
        return ShrinkWrap.create(JavaArchive.class, NoAuditLogTestCase.class.getName() + ".jar")
                .addClass(ConnectionHoldingBean.class)
                .addClass(RemoteConnectionHolding.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, create("beans.xml"));
    }

    @Test
    public void testNoAuditMessagesLogged() throws Exception {
        try (BufferedReader reader = Files.newBufferedReader(getLogFile(), Charset.defaultCharset())) {
            String line;
            while ((line = reader.readLine()) != null) {
                Assert.assertFalse(String.format("Log contains ActiveMQ audit log messages: %n%s", line), line.contains("org.apache.activemq.audit"));
            }
        }
    }

    private Path getLogFile() throws IOException {
        final ModelNode address = Operations.createAddress("subsystem", "logging", "periodic-rotating-file-handler", "FILE");
        final ModelNode op = Operations.createOperation("resolve-path", address);
        final ModelNode result = managementClient.getControllerClient().execute(op);
        if (!Operations.isSuccessfulOutcome(result)) {
            Assert.fail("Failed to locate the log file: " + Operations.getFailureDescription(result).asString());
        }
        return Paths.get(Operations.readResult(result).asString());
    }
}
