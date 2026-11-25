/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.subsystem;

import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredAddStepHandler;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
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
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Assert;
import org.junit.Test;

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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

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
        final String cp = System.getProperty("java.class.path", ".");
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
        // should not be used
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        throw new UnsupportedOperationException();
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
                    extensionRegistration.registerReadOnlyAttribute(new SimpleAttributeDefinitionBuilder("module", ModelType.STRING).setRequired(true).build(), null);
                    extensionRegistration.registerOperationHandler(removeExtension, ReloadRequiredRemoveStepHandler.INSTANCE);
                    extensionRegistration.registerOperationHandler(addExtension,
                            new ReloadRequiredAddStepHandler(
                                    new SimpleAttributeDefinitionBuilder("module", ModelType.STRING).setRequired(true).build()));

                    final OperationDefinition removeSubsystem = new SimpleOperationDefinitionBuilder("remove", NonResolvingResourceDescriptionResolver.INSTANCE)
                            .build();

                    final OperationDefinition addSubsystem = new SimpleOperationDefinitionBuilder("add", NonResolvingResourceDescriptionResolver.INSTANCE)
                            .build();

                    PathElement bvSubsystem = PathElement.pathElement(SUBSYSTEM, "bean-validation");
                    ManagementResourceRegistration subsystemRegistration = rootRegistration.registerSubModel(new SimpleResourceDefinition(bvSubsystem, NonResolvingResourceDescriptionResolver.INSTANCE));
                    subsystemRegistration.registerOperationHandler(removeSubsystem, ReloadRequiredRemoveStepHandler.INSTANCE);
                    subsystemRegistration.registerOperationHandler(addSubsystem, new ReloadRequiredAddStepHandler());
                }

            }
        };

    }
}
