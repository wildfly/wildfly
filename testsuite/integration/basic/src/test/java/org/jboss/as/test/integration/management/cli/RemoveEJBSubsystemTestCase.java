/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.management.cli;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

/**
 * Test removes EJB subsystem using the CLI command and checks if no error occurred.
 *
 * Automated test for [ WFLY-9405 ]
 *
 * @author Daniel Cihak
 */
@RunWith(Arquillian.class)
@ServerSetup(SnapshotRestoreSetupTask.class)
@RunAsClient
public class RemoveEJBSubsystemTestCase {

    private static final String DEPLOYMENT = "deployment";

    @ContainerResource
    private ManagementClient managementClient;

    @Deployment(name = DEPLOYMENT)
    public static WebArchive createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
        war.addClass(RemoveEJBSubsystemTestCase.class);
        return war;
    }

    @Test
    public void testRemoveEJBSubsystem() throws Exception {
        // /subsystem=ejb3/service=remote/channel-creation-options=MAX_OUTBOUND_MESSAGES:remove
        ModelNode removeOp = new ModelNode();
        removeOp.get(OP).set(REMOVE);
        removeOp.get(OP_ADDR).add("subsystem", "ejb3");
        removeOp.get(OP_ADDR).add("service", "remote");
        removeOp.get(OP_ADDR).add("channel-creation-options", "MAX_OUTBOUND_MESSAGES");
        CoreUtils.applyUpdate(removeOp, managementClient.getControllerClient());
    }
}
