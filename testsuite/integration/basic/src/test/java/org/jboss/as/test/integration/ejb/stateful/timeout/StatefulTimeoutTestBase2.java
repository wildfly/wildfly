/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.timeout;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.io.FilePermission;

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
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

abstract class StatefulTimeoutTestBase2 extends StatefulTimeoutTestBase1 {
    static final PathAddress EJB3_ADDRESS = PathAddress.pathAddress("subsystem", "ejb3");
    static final String DEFAULT_STATEFUL_TIMEOUT_NAME = "default-stateful-bean-session-timeout";

    @ArquillianResource
    static ManagementClient managementClient;

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addPackage(StatefulTimeoutTestBase1.class.getPackage());

        jar.addClasses(ModelNode.class, PathAddress.class, ManagementOperations.class, MgmtOperationException.class);
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.remoting, org.jboss.as.controller\n"), "MANIFEST.MF");
        jar.addAsManifestResource(createPermissionsXmlAsset(
                new RemotingPermission("createEndpoint"),
                new RemotingPermission("connect"),
                new FilePermission(System.getProperty("jboss.inst") + "/standalone/tmp/auth/*", "read")
        ), "permissions.xml");

        jar.add(new StringAsset(DEPLOYMENT_DESCRIPTOR_CONTENT), "META-INF/ejb-jar.xml");
        jar.add(EmptyAsset.INSTANCE, "META-INF/beans.xml");
        return jar;
    }

    static ModelNode readAttribute() throws Exception {
        return executeOperation(Util.getReadAttributeOperation(EJB3_ADDRESS, DEFAULT_STATEFUL_TIMEOUT_NAME));
    }

    static ModelNode writeAttribute(int value) throws Exception {
        return executeOperation(Util.getWriteAttributeOperation(EJB3_ADDRESS, DEFAULT_STATEFUL_TIMEOUT_NAME, value));
    }

    static ModelNode executeOperation(ModelNode op) throws Exception {
        return ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
    }
}
