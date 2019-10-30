/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2015, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */
package org.jboss.as.ee.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.ee.subsystem.GlobalModulesDefinition.ANNOTATIONS;
import static org.jboss.as.ee.subsystem.GlobalModulesDefinition.META_INF;
import static org.jboss.as.ee.subsystem.GlobalModulesDefinition.NAME;
import static org.jboss.as.ee.subsystem.GlobalModulesDefinition.SERVICES;
import static org.jboss.as.ee.subsystem.GlobalModulesDefinition.SLOT;
import static org.jboss.as.ee.subsystem.GlobalModulesDefinition.GLOBAL_MODULES;
import static org.jboss.as.ee.subsystem.EESubsystemModel.ANNOTATION_PROPERTY_REPLACEMENT;
import static org.jboss.as.ee.subsystem.EESubsystemModel.EAR_SUBDEPLOYMENTS_ISOLATED;
import static org.jboss.as.ee.subsystem.EESubsystemModel.JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT;
import static org.jboss.as.ee.subsystem.EESubsystemModel.SPEC_DESCRIPTOR_PROPERTY_REPLACEMENT;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredAddStepHandler;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="opalka.richard@gmail.com">Richard Opalka</a>
 */
public class EeLegacySubsystemTestCase extends AbstractSubsystemBaseTest {

    public EeLegacySubsystemTestCase() {
        super(EeExtension.SUBSYSTEM_NAME, new EeExtension());
    }

    @Test
    public void testLegacyConfigurations() throws Exception {
        // Get a list of all the logging_x_x.xml files
        final Pattern pattern = Pattern.compile("(subsystem)_\\d+_\\d+\\.xml");
        // Using the CP as that's the standardSubsystemTest will use to find the config file
        final String cp = WildFlySecurityManager.getPropertyPrivileged("java.class.path", ".");
        final String[] entries = cp.split(Pattern.quote(File.pathSeparator));
        final List<String> configs = new ArrayList<>();
        for (String entry : entries) {
            final Path path = Paths.get(entry);
            if (Files.isDirectory(path)) {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                        final String name = file.getFileName().toString();
                        if (pattern.matcher(name).matches()) {
                            configs.add(name);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }

        // The paths shouldn't be empty
        Assert.assertFalse("No configs were found", configs.isEmpty());

        for (String configId : configs) {
            // Run the standard subsystem test, but don't compare the XML as it should never match
            standardSubsystemTest(configId, false);
        }
    }

    @Test
    public void testSubsystem() throws Exception {
        KernelServices services = standardSubsystemTest("subsystem_1_2.xml", false);
        ModelNode model = services.readWholeModel();
        ModelNode subsystem = model.require(SUBSYSTEM).require(EeExtension.SUBSYSTEM_NAME);
        ModelNode globalModules = subsystem.require(GLOBAL_MODULES);

        assertEquals("org.jboss.logging", globalModules.require(0).require(NAME).asString());
        assertEquals("main", globalModules.require(0).require(SLOT).asString());

        assertEquals("org.apache.log4j", globalModules.require(1).require(NAME).asString());
        assertTrue(globalModules.require(1).require(ANNOTATIONS).asBoolean());
        assertTrue(globalModules.require(1).require(META_INF).asBoolean());
        assertFalse(globalModules.require(1).require(SERVICES).asBoolean());

        assertFalse(subsystem.require(ANNOTATION_PROPERTY_REPLACEMENT).asBoolean());
        assertTrue(subsystem.require(EAR_SUBDEPLOYMENTS_ISOLATED).asBoolean());
        assertTrue(subsystem.require(JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT).asBoolean());
        assertFalse(subsystem.require(SPEC_DESCRIPTOR_PROPERTY_REPLACEMENT).asBoolean());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_1_2.xml");
    }

    @Override
    protected String getSubsystemXml(String resource) throws IOException {
        return readResource(resource);
    }

    @Override
    protected void compare(ModelNode node1, ModelNode node2) {
        node1.remove(EXTENSION);
        node2.remove(EXTENSION);
        node1.get(SUBSYSTEM).remove("bean-validation");
        node2.get(SUBSYSTEM).remove("bean-validation");
        super.compare(node1, node2);
    }

    boolean extensionAdded = false;
    @Override
    protected AdditionalInitialization createAdditionalInitialization() {

        return new AdditionalInitialization() {

            @Override
            protected RunningMode getRunningMode() {
                return RunningMode.ADMIN_ONLY;
            }

            @Override
            protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource, ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {
                if (!extensionAdded) {
                    //extensionAdded = true;
                    //bean validation depends on EE, so we can't use the real subsystem here
                    final OperationDefinition removeExtension = new SimpleOperationDefinitionBuilder("remove", NonResolvingResourceDescriptionResolver.INSTANCE)
                            .build();

                    final OperationDefinition addExtension = new SimpleOperationDefinitionBuilder("add", NonResolvingResourceDescriptionResolver.INSTANCE)
                            .addParameter(new SimpleAttributeDefinitionBuilder("module", ModelType.STRING).setRequired(true).build())
                            .build();

                    PathElement bvExtension = PathElement.pathElement(EXTENSION, "org.wildfly.extension.bean-validation");
                    ManagementResourceRegistration extensionRegistration = rootRegistration.registerSubModel(new SimpleResourceDefinition(bvExtension, NonResolvingResourceDescriptionResolver.INSTANCE));
                    extensionRegistration.registerReadOnlyAttribute(new SimpleAttributeDefinitionBuilder("module", ModelType.STRING).setRequired(true).build(), new ReloadRequiredWriteAttributeHandler());
                    extensionRegistration.registerOperationHandler(removeExtension, new ReloadRequiredRemoveStepHandler());
                    extensionRegistration.registerOperationHandler(addExtension,
                            new ReloadRequiredAddStepHandler(
                                    new SimpleAttributeDefinitionBuilder("module", ModelType.STRING).setRequired(true).build()));

                    final OperationDefinition removeSubsystem = new SimpleOperationDefinitionBuilder("remove", NonResolvingResourceDescriptionResolver.INSTANCE)
                            .build();

                    final OperationDefinition addSubsystem = new SimpleOperationDefinitionBuilder("add", NonResolvingResourceDescriptionResolver.INSTANCE)
                            .build();

                    PathElement bvSubsystem = PathElement.pathElement(SUBSYSTEM, "bean-validation");
                    ManagementResourceRegistration subsystemRegistration = rootRegistration.registerSubModel(new SimpleResourceDefinition(bvSubsystem, NonResolvingResourceDescriptionResolver.INSTANCE));
                    subsystemRegistration.registerOperationHandler(removeSubsystem, new ReloadRequiredRemoveStepHandler());
                    subsystemRegistration.registerOperationHandler(addSubsystem, new ReloadRequiredAddStepHandler());
                }

            }
        };

    }
}
