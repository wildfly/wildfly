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
package org.jboss.as.messaging.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.ExtensionRegistryType;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.messaging.MessagingExtension;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
public class MigrateTestCase extends AbstractSubsystemTest {

    public static final String MESSAGING_ACTIVEMQ_SUBSYSTEM_NAME = "messaging-activemq";

    public MigrateTestCase() {
        super(MessagingExtension.SUBSYSTEM_NAME, new MessagingExtension());
    }

    @Test
    public void testMigrateOperationWithoutLegacyEntries() throws Exception {
        testMigrateOperation(false);
    }

    @Test
    public void testMigrateOperationWithLegacyEntries() throws Exception {
        testMigrateOperation(true);
    }

    private void testMigrateOperation(boolean addLegacyEntries) throws Exception {
        String subsystemXml = readResource("subsystem_migration.xml");
        newSubsystemAdditionalInitialization additionalInitialization = new newSubsystemAdditionalInitialization();
        KernelServices services = createKernelServicesBuilder(additionalInitialization).setSubsystemXml(subsystemXml).build();

        ModelNode model = services.readWholeModel();
        assertFalse(additionalInitialization.extensionAdded);
        assertTrue(model.get(SUBSYSTEM, MessagingExtension.SUBSYSTEM_NAME).isDefined());
        assertFalse(model.get(SUBSYSTEM, MESSAGING_ACTIVEMQ_SUBSYSTEM_NAME).isDefined());

        ModelNode migrateOp = new ModelNode();
        migrateOp.get(OP).set("migrate");
        migrateOp.get("add-legacy-entries").set(addLegacyEntries);
        migrateOp.get(OP_ADDR).add(SUBSYSTEM, MessagingExtension.SUBSYSTEM_NAME);

        checkOutcome(services.executeOperation(migrateOp));

        model = services.readWholeModel();

        assertTrue(additionalInitialization.extensionAdded);
        assertFalse(model.get(SUBSYSTEM, MessagingExtension.SUBSYSTEM_NAME).isDefined());
        assertTrue(model.get(SUBSYSTEM, MESSAGING_ACTIVEMQ_SUBSYSTEM_NAME).isDefined());

        ModelNode newSubsystem = model.get(SUBSYSTEM, MESSAGING_ACTIVEMQ_SUBSYSTEM_NAME);
        ModelNode newServer = newSubsystem.get("server", "default");
        assertNotNull(newServer);
        assertTrue(newServer.isDefined());

        if (addLegacyEntries) {
            // check that legacy entries were added to JMS resources
            ModelNode jmsQueueLegacyEntries = newServer.get("jms-queue", "testQueue", "legacy-entries");
            assertTrue(jmsQueueLegacyEntries.toJSONString(true), jmsQueueLegacyEntries.isDefined());
            assertEquals(1, jmsQueueLegacyEntries.asList().size());

            ModelNode jmsTopicLegacyEntries = newServer.get("jms-topic", "testTopic", "legacy-entries");
            assertTrue(jmsTopicLegacyEntries.toJSONString(true), jmsQueueLegacyEntries.isDefined());
            assertEquals(1, jmsTopicLegacyEntries.asList().size());

            assertTrue(newServer.get("legacy-connection-factory", "RemoteConnectionFactory").isDefined());
        } else {
            // check that no legacy resource or entries were added to the new server
            assertFalse(newServer.get("jms-queue", "testQueue", "legacy-entries").isDefined());
            assertFalse(newServer.get("jms-topic", "testTopic", "legacy-entries").isDefined());
            assertFalse(newServer.get("jms-topic", "testTopic", "legacy-entries").isDefined());
            assertFalse(newServer.get("legacy-connection-factory", "RemoteConnectionFactory").isDefined());
        }
    }

    private static class newSubsystemAdditionalInitialization extends AdditionalInitialization {

        org.wildfly.extension.messaging.activemq.MessagingExtension newSubsystem = new org.wildfly.extension.messaging.activemq.MessagingExtension();
        boolean extensionAdded = false;

        @Override
        protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource, ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {
            rootRegistration.registerSubModel(new SimpleResourceDefinition(PathElement.pathElement(EXTENSION),
                    ControllerResolver.getResolver(EXTENSION), new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    extensionAdded = true;
                    newSubsystem.initialize(extensionRegistry.getExtensionContext("org.wildfly.extension.messaging-activemq",
                            rootRegistration, ExtensionRegistryType.SERVER));
                }
            }, null));
        }

        @Override
        protected RunningMode getRunningMode() {
            return RunningMode.ADMIN_ONLY;
        }
    }
}
