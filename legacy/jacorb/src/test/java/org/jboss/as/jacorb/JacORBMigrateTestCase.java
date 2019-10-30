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
package org.jboss.as.jacorb;

import static org.jboss.as.controller.capability.RuntimeCapability.buildDynamicCapabilityName;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.ExtensionRegistryType;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.jboss.security.SecurityDomain;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.iiop.openjdk.IIOPExtension;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author <a href=mailto:tadamski@redhat.com>Tomasz Adamski</a>
 */
public class JacORBMigrateTestCase extends AbstractSubsystemTest {

    public JacORBMigrateTestCase() {
        super(JacORBExtension.SUBSYSTEM_NAME, new JacORBExtension());
    }

    @Test
    public void migrateTest() throws Exception {

        String subsystemXml = readResource("subsystem.xml");
        NewSubsystemAdditionalInitialization additionalInitialization = new NewSubsystemAdditionalInitialization();
        KernelServices services = createKernelServicesBuilder(additionalInitialization).setSubsystemXml(subsystemXml).build();

        ModelNode model = services.readWholeModel();
        Assert.assertFalse(additionalInitialization.extensionAdded);
        Assert.assertTrue(model.get(SUBSYSTEM).hasDefined(JacORBExtension.SUBSYSTEM_NAME));
        Assert.assertFalse(model.get(SUBSYSTEM).hasDefined(IIOPExtension.SUBSYSTEM_NAME));

        ModelNode migrateOp = new ModelNode();
        migrateOp.get(OP).set("migrate");
        migrateOp.get(OP_ADDR).set(
                PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, JacORBExtension.SUBSYSTEM_NAME)).toModelNode());

        checkOutcome(services.executeOperation(migrateOp));

        model = services.readWholeModel();

        Assert.assertTrue(additionalInitialization.extensionAdded);
        Assert.assertFalse(model.get(SUBSYSTEM).hasDefined(JacORBExtension.SUBSYSTEM_NAME));
        Assert.assertTrue(model.get(SUBSYSTEM).hasDefined(IIOPExtension.SUBSYSTEM_NAME));

        ModelNode newSubsystem = model.get(SUBSYSTEM).get("iiop-openjdk");
        Assert.assertTrue(newSubsystem.get("export-corbaloc").equals(new ModelNode(true)));
        Assert.assertTrue(newSubsystem.get("confidentiality").equals(new ModelNode("required")));
        Assert.assertTrue(newSubsystem.get("iona").equals(new ModelNode(true)));
    }

    private static class NewSubsystemAdditionalInitialization extends AdditionalInitialization {
        IIOPExtension newSubsystem = new IIOPExtension();
        boolean extensionAdded = false;
        @Override
        protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource, ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {

            final OperationDefinition removeExtension = new SimpleOperationDefinitionBuilder("remove", new StandardResourceDescriptionResolver("test", "test", getClass().getClassLoader()))
                    .build();

            PathElement webExtension = PathElement.pathElement(EXTENSION, "org.jboss.as.jacorb");
            rootRegistration.registerSubModel(new SimpleResourceDefinition(webExtension, ControllerResolver.getResolver(EXTENSION)))
                    .registerOperationHandler(removeExtension, new ReloadRequiredRemoveStepHandler());
            rootResource.registerChild(webExtension, Resource.Factory.create());

            rootRegistration.registerSubModel(new SimpleResourceDefinition(PathElement.pathElement(EXTENSION),
                    ControllerResolver.getResolver(EXTENSION), new OperationStepHandler() {
                        @Override
                        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                            extensionAdded = true;
                            newSubsystem.initialize(extensionRegistry.getExtensionContext("org.wildfly.iiop-openjdk",
                                    rootRegistration, ExtensionRegistryType.SLAVE));
                        }
                    }, null));

            Map<String, Class> capabilities = new HashMap<>();
            capabilities.put(buildDynamicCapabilityName("org.wildfly.security.legacy-security-domain", "security-domain"), SecurityDomain.class);
            registerServiceCapabilities(capabilityRegistry, capabilities);
        }

        @Override
        protected RunningMode getRunningMode() {
            return RunningMode.ADMIN_ONLY;
        }

        @Deprecated
        protected ProcessType getProcessType() {
                    return ProcessType.HOST_CONTROLLER;
                }
    }
}
