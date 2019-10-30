/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.manualmode.model;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import javax.inject.Inject;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.core.testrunner.ManagementClient;
import org.wildfly.core.testrunner.Server;
import org.wildfly.core.testrunner.ServerControl;
import org.wildfly.core.testrunner.ServerController;
import org.wildfly.core.testrunner.WildflyTestRunner;

/**
 * Ensures that the full model including runtime resources/attributes can be read in both normal and admin-only mode
 * for the fullest server config possible
 *
 * @author Kabir Khan
 * @author Tomaz Cerar
 */
@ServerControl(manual = true)
@RunWith(WildflyTestRunner.class)
public class ReadFullModelTestCase {

    //This is the full-ha setup which is the fullest config we have
    //Reuse this rather than a setup
    private static final String SERVER_CONFIG = "standalone-full-ha.xml";

    @Inject
    private ServerController container;


    @Test
    public void test() throws Exception {
        container.start(SERVER_CONFIG, Server.StartMode.NORMAL);
        try {
            ManagementClient client = container.getClient();
            ModelNode rr = Util.createEmptyOperation(READ_RESOURCE_OPERATION, PathAddress.EMPTY_ADDRESS);
            rr.get(INCLUDE_RUNTIME).set(true);
            rr.get(RECURSIVE).set(true);
            ModelNode result = client.executeForResult(rr);

            //Just a quick sanity test to check that we are full-ha by making sure some of the expected subsystems are there
            Assert.assertTrue(result.hasDefined(SUBSYSTEM, "messaging-activemq"));
            Assert.assertTrue(result.hasDefined(SUBSYSTEM, "jgroups"));
            container.reload();
            client.executeForResult(rr);
        } finally {
            container.stop();
        }
    }

}
