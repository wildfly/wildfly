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

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Ensures that the full model including runtime resources/attributes can be read in both normal and admin-only mode
 * for the fullest server config possible
 *
 * @author Kabir Khan
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ReadFullModelTestCase {

    //This is the full-ha setup which is the fullest config we have
    //Reuse this rather than a setup
    public static final String SERVER = "jbossas-messaging-ha-server1";

    @ArquillianResource
    protected static ContainerController controller;

    protected static ModelControllerClient createClient1() {
        return TestSuiteEnvironment.getModelControllerClient();
    }


    @Test
    public void test() throws Exception {
        controller.start(SERVER);
        try {
            ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
            ModelNode rr = Util.createEmptyOperation(READ_RESOURCE_OPERATION, PathAddress.EMPTY_ADDRESS);
            rr.get(INCLUDE_RUNTIME).set(true);
            rr.get(RECURSIVE).set(true);
            ModelNode result = ModelTestUtils.checkResultAndGetContents(client.execute(rr));

            //Just a quick sanity test to check that we are full-ha by making sure some of the expected subsystems are there
            Assert.assertTrue(result.hasDefined(SUBSYSTEM, "messaging-activemq"));
            Assert.assertTrue(result.hasDefined(SUBSYSTEM, "jgroups"));

            ServerReload.executeReloadAndWaitForCompletion(client, true);
            result = ModelTestUtils.checkResultAndGetContents(client.execute(rr));
        } finally {
            if (controller.isStarted(SERVER)) {
                controller.stop(SERVER);
            }
        }
    }

}
