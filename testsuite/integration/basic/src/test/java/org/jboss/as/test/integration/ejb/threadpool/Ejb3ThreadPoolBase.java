/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.threadpool;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.io.FilePermission;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.security.RemotingPermission;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;

public class Ejb3ThreadPoolBase {

    static PathAddress DEFAULT_THREAD_POOL_ADDRESS = PathAddress.pathAddress("subsystem", "ejb3").append("thread-pool", "default");

    @ArquillianResource
    static ManagementClient managementClient;

    @ArquillianResource
    InitialContext iniCtx;

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb3singleton.jar");
        jar.addClass(ScheduleSingletonOneTimer.class);
        jar.addClasses(Ejb3ThreadPoolBase.class, Ejb3NonCoreThreadTimeoutTestCase.class, ModelNode.class, PathAddress.class,
                ManagementOperations.class, MgmtOperationException.class);
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.remoting, org.jboss.as.controller\n"), "MANIFEST.MF");
        jar.addAsManifestResource(createPermissionsXmlAsset(
                new RemotingPermission("createEndpoint"),
                new RemotingPermission("connect"),
                new FilePermission(System.getProperty("jboss.inst") + "/standalone/tmp/auth/*", "read")
        ), "permissions.xml");
        return jar;
    }

    void waitUntilThreadPoolProcessedAtLeast(int tasks, long timeoutInMillis) throws Exception {
        long startTime = System.currentTimeMillis();
        ModelNode readTasks = Util.getReadAttributeOperation(DEFAULT_THREAD_POOL_ADDRESS, "completed-task-count");
        int actualTaskCount;
        while ((actualTaskCount = executeOperation(readTasks).asInt()) < tasks) {
            if (System.currentTimeMillis() - startTime > timeoutInMillis) {
                Assert.fail("There are not enough tasks (expected: " + tasks + ", actual: " + actualTaskCount + ") processed by thread pool in timeout " + timeoutInMillis);
            }
            Thread.sleep(500);
        }
    }

    ModelNode readAttribute(String attribute) throws Exception {
        return executeOperation(Util.getReadAttributeOperation(DEFAULT_THREAD_POOL_ADDRESS, attribute));
    }

    static ModelNode executeOperation(ModelNode op) throws Exception {
        ModelNode result = ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
        return result;
    }
}
