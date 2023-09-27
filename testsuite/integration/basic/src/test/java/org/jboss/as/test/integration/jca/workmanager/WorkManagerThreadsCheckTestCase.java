/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.workmanager;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.AllOf.allOf;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;

import java.io.IOException;

import org.hamcrest.MatcherAssert;
import org.jboss.arquillian.container.test.api.Deployment;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.jca.JcaMgmtBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Checks that only one short-running-thread pool or long-running-thread pool is allowed under one workmanager.
 *
 * @author Lin Gao
 */
@RunWith(Arquillian.class)
@RunAsClient
public class WorkManagerThreadsCheckTestCase extends JcaMgmtBase {

    private static final PathAddress WORKMANAGER_ADDRESS = PathAddress.pathAddress("subsystem", "jca").append("workmanager", "default");

    @Deployment
    public static Archive<?> deploytRar() {
        return ShrinkWrap.create(JavaArchive.class, "dummy.jar");
    }

    @Test
    public void testOneLongRunningThreadPool() throws IOException {
        ModelNode operation = Operations.createAddOperation(WORKMANAGER_ADDRESS.append("long-running-threads", "Long").toModelNode());
        operation.get("max-threads").set(10);
        operation.get("queue-length").set(10);
        try {
            executeOperation(operation);
            Assert.fail("NOT HERE!");
        } catch (MgmtOperationException e) {
            String reason = e.getResult().get("failure-description").asString();
            MatcherAssert.assertThat("Wrong error message", reason, allOf(
                    containsString("WFLYJCA0101"),
                    containsString("Long"),
                    containsString("long-running-threads"),
                    containsString("default")
            ));
        }
    }

    @Test
    public void testOneShortRunningThreadPool() throws IOException {
        ModelNode operation = Operations.createAddOperation(WORKMANAGER_ADDRESS.append("short-running-threads", "Short").toModelNode());
        operation.get("max-threads").set(10);
        operation.get("queue-length").set(10);
        try {
            executeOperation(operation);
            Assert.fail("NOT HERE!");
        } catch (MgmtOperationException e) {
            String reason = e.getResult().get("failure-description").asString();
            MatcherAssert.assertThat("Wrong error message", reason, allOf(
                    containsString("WFLYJCA0101"),
                    containsString("Short"),
                    containsString("short-running-threads"),
                    containsString("default")
            ));
        }
    }

    @Test
    public void testShortRunningThreadPoolName() throws Exception {
        PathAddress address = PathAddress.pathAddress("subsystem", "jca").append("workmanager", "name-test");
        ModelNode operation = Operations.createAddOperation(address.toModelNode());
        operation.get("name").set("name-test");
        executeOperation(operation);
        operation = Operations.createAddOperation(address.append("short-running-threads", "Short").toModelNode());
        operation.get("max-threads").set(10);
        operation.get("queue-length").set(10);
        try {
            executeOperation(operation);
            Assert.fail("NOT HERE!");
        } catch (MgmtOperationException e) {
            String reason = e.getResult().get("failure-description").asString();
            MatcherAssert.assertThat("Wrong error message", reason, allOf(
                    containsString("WFLYJCA0122"),
                    containsString("Short"),
                    containsString("short-running-threads"),
                    containsString("name-test")
            ));
        } finally {
            executeOperation(Operations.createRemoveOperation(address.toModelNode()));
        }
    }

    @Test
    public void testBlockingThreadPool() throws IOException {
        Assert.assertTrue(isBlockingThreadPool("short-running-threads"));
        Assert.assertTrue(isBlockingThreadPool("long-running-threads"));
    }

    protected boolean isBlockingThreadPool(String pool) throws IOException {
        ModelNode operation = Operations.createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, WORKMANAGER_ADDRESS.append(pool, "default").toModelNode());
        try {
            ModelNode result = executeOperation(operation);
            return result.asString().contains("A thread pool executor with a bounded queue where threads submittings tasks may block");
        } catch (MgmtOperationException e) {
            return false;
        }
    }
}
