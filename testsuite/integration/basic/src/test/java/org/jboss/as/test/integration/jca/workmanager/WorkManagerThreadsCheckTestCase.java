/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.jca.workmanager;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.AllOf.allOf;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;

import java.io.IOException;
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
            Assert.assertThat("Wrong error message", reason, allOf(
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
            Assert.assertThat("Wrong error message", reason, allOf(
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
            Assert.assertThat("Wrong error message", reason, allOf(
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
