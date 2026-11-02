/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.hashicorp.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;

/** Verifies that the Vault bootable profile actually launches the packaged JAR. */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
@EnabledIfSystemProperty(named = "bootable.jar", matches = ".+")
public class BootableJarLaunchTestCase {

    @Deployment
    public static JavaArchive deployment() {
        return ShrinkWrap.create(JavaArchive.class, "vault-bootable-launch-test.jar");
    }

    @Test
    public void testBootableJarLaunch(@ArquillianResource ManagementClient managementClient) throws Exception {
        PathAddress runtime = PathAddress.pathAddress("core-service", "platform-mbean").append("type", "runtime");
        ModelNode response = managementClient.getControllerClient().execute(
                Util.getReadAttributeOperation(runtime, "system-properties"));
        assertEquals("success", response.get("outcome").asString(), response.toString());
        ModelNode properties = response.get("result");
        assertEquals(Path.of(System.getProperty("bootable.jar")).toRealPath(),
                Path.of(properties.get("java.class.path").asString()).toRealPath(),
                "The server must run from the bootable JAR");
        assertEquals(Path.of(System.getProperty("jboss.home")).toRealPath(),
                Path.of(properties.get("jboss.home.dir").asString()).toRealPath(),
                "The imported TLS tests must write stores into the running server");
    }
}
