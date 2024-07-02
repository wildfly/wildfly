/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.controller.PathElement;
import org.jgroups.Global;
import org.jgroups.stack.Protocol;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public class GenericProtocolResourceDefinition extends ProtocolResourceDefinition<Protocol> {

    public static PathElement pathElement(String name) {
        return ProtocolResourceDefinition.pathElement(Global.PREFIX + name);
    }

    GenericProtocolResourceDefinition(UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfigurator parentServiceConfigurator) {
        this(WILDCARD_PATH, configurator, parentServiceConfigurator);
    }

    GenericProtocolResourceDefinition(String name, JGroupsSubsystemModel deprecation, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfigurator parentServiceConfigurator) {
        this(pathElement(name), configurator, parentServiceConfigurator);
        this.setDeprecated(deprecation.getVersion());
    }

    private GenericProtocolResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfigurator parentServiceConfigurator) {
        super(path, configurator, parentServiceConfigurator);
    }
}
