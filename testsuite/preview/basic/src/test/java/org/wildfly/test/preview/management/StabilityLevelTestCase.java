/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.preview.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test of stability level behavior for WildFly Preview.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class StabilityLevelTestCase {

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment()
    public static WebArchive createDeployment1() {
        return AssumeTestGroupUtil.emptyWar();
    }

    /**
     * Tests that the server has the expected stability level and permissible stability levels.
     *
     * @throws IOException if a problem occurs connecting to the server
     */
    @Test
    public void testOOTBStability() throws IOException {
        ModelNode op = Util.getReadAttributeOperation(PathAddress.pathAddress("core-service", "server-environment"), "stability");
        ModelNode response = managementClient.getControllerClient().execute(op);
        assertEquals(response.toString(), "preview", response.get("result").asString());

        op.get("name").set("permissible-stability-levels");
        response = managementClient.getControllerClient().execute(op);
        Set<Stability> stabilities = EnumSet.allOf(Stability.class);
        ModelNode result = response.get("result");
        assertEquals(response.toString(), stabilities.size(), result.asInt());
        for (ModelNode permissible : result.asList()) {
            assertTrue(permissible.asString(), stabilities.contains(Stability.fromString(permissible.asString())));
        }
    }
}
