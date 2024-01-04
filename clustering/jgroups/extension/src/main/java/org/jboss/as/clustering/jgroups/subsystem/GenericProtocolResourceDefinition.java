/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.controller.PathElement;
import org.jgroups.Global;

/**
 * @author Paul Ferraro
 */
public class GenericProtocolResourceDefinition extends ProtocolResourceDefinition {

    public static PathElement pathElement(String name) {
        return ProtocolResourceDefinition.pathElement(Global.PREFIX + name);
    }

    GenericProtocolResourceDefinition(UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfiguratorFactory parentServiceConfiguratorFactory) {
        this(WILDCARD_PATH, configurator, parentServiceConfiguratorFactory);
    }

    GenericProtocolResourceDefinition(String name, JGroupsSubsystemModel deprecation, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfiguratorFactory parentServiceConfiguratorFactory) {
        this(pathElement(name), configurator, parentServiceConfiguratorFactory);
        this.setDeprecated(deprecation.getVersion());
    }

    private GenericProtocolResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfiguratorFactory parentServiceConfiguratorFactory) {
        super(path, configurator, ProtocolConfigurationServiceConfigurator::new, parentServiceConfiguratorFactory);
    }
}
