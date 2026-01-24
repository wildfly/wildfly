/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ModuleAttributeDefinition;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleLoadException;
import org.jgroups.Global;
import org.jgroups.JChannel;
import org.jgroups.stack.Protocol;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.PropertiesAttributeDefinition;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.StatisticsEnabledAttributeDefinition;
import org.wildfly.subsystem.resource.PropertiesAttributeDefinition.PropertyValueContextPersistence;

/**
 * Registers a resource definition for a typed JGroups protocol component.
 *
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public class ProtocolChildResourceDefinitionRegistrar implements ChildResourceDefinitionRegistrar, UnaryOperator<ResourceDescriptor.Builder> {
    static final ModuleAttributeDefinition MODULE = new ModuleAttributeDefinition.Builder().setDefaultValue(Module.forClass(JChannel.class)).build();
    static final PropertiesAttributeDefinition PROPERTIES = new PropertiesAttributeDefinition.Builder()
            .setPropertyPersistence(new PropertyValueContextPersistence(ModelDescriptionConstants.NAME))
            .build();
    static final StatisticsEnabledAttributeDefinition STATISTICS_ENABLED = new StatisticsEnabledAttributeDefinition.Builder().setDefaultValue(null).build();

    interface Configurator {
        ResourceRegistration getResourceRegistration();

        ResourceDescriptionResolver getResourceDescriptionResolver();

        default UnaryOperator<ResourceDefinition.Builder> getResourceDefinitionConfigurator() {
            return UnaryOperator.identity();
        }

        default JGroupsSubsystemModel getDeprecation() {
            return null;
        }
    }

    private final Configurator configurator;

    ProtocolChildResourceDefinitionRegistrar(Configurator configurator) {
        this.configurator = configurator;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = this.configurator.getResourceDescriptionResolver();
        ResourceDescriptor descriptor = this.apply(ResourceDescriptor.builder(resolver)).build();
        ResourceDefinition definition = this.configurator.getResourceDefinitionConfigurator().apply(ResourceDefinition.builder(this.configurator.getResourceRegistration(), resolver, this.configurator.getDeprecation())).build();

        ManagementResourceRegistration registration = parent.registerSubModel(definition);

        ManagementResourceRegistrar.of(descriptor).register(registration);

        return registration;
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return builder.addAttributes(List.of(MODULE, PROPERTIES, STATISTICS_ENABLED));
    }

    static Class<? extends Protocol> findProtocolClass(ExpressionResolver context, String protocolName, ModelNode protocolModel) throws OperationFailedException {
        String moduleName = MODULE.resolveModelAttribute(context, protocolModel).asString();

        ModuleClassLoader classLoader;
        try {
            classLoader = Module.getContextModuleLoader().loadModule(moduleName).getClassLoader();
        } catch (ModuleLoadException e) {
            throw JGroupsLogger.ROOT_LOGGER.unableToLoadProtocolModule(moduleName, protocolName);
        }

        try {
            return findProtocolClass(classLoader, protocolName);
        } catch (ClassNotFoundException e) {
            throw JGroupsLogger.ROOT_LOGGER.unableToLoadProtocolClass(protocolName);
        }
    }

    static Class<? extends Protocol> findProtocolClass(ClassLoader classLoader, String protocolName) throws ClassNotFoundException {
        List<String> candidateClassNames = new ArrayList<>(2);

        if (protocolName.startsWith(Global.PREFIX)) {
            // Protocol name is already a jgroups protocol class name
            candidateClassNames.add(protocolName);
        } else {
            boolean isDefaultModule = Protocol.class.getClassLoader().equals(classLoader);

            // If using non-default module, try loading protocol name as class name first
            if (!isDefaultModule) {
                candidateClassNames.add(protocolName);
            }
            // Compose class name using standard prefix
            // e.g. "raft.RAFT" akin to standalone jgroups classloading
            candidateClassNames.add(Global.PREFIX + protocolName);
        }

        Iterator<String> classNames = candidateClassNames.iterator();
        ClassNotFoundException firstException = null;
        while (classNames.hasNext()) {
            try {
                return classLoader.loadClass(classNames.next()).asSubclass(Protocol.class);
            } catch (ClassNotFoundException e) {
                // Retry with next
                if (firstException == null) {
                    firstException = e;
                }
            }
        }
        throw firstException;
    }
}
