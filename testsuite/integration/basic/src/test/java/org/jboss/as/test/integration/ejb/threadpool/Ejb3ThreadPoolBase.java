/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.threadpool;

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

import javax.naming.InitialContext;
import java.io.FilePermission;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

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
        while (executeOperation(readTasks).asInt() < tasks) {
            if (System.currentTimeMillis() - startTime > timeoutInMillis) {
                Assert.fail("There are not enough tasks (expected: " + tasks + ") processed by thread pool in timeout " + timeoutInMillis);
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
