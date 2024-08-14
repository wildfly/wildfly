/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.stream.Stream;

import org.jboss.as.clustering.controller.ModuleAttributeDefinition;
import org.jboss.as.clustering.controller.PropertiesAttributeDefinition;
import org.jboss.as.clustering.controller.ResourceDescription;
import org.jboss.as.clustering.controller.StatisticsEnabledAttributeDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.modules.Module;
import org.jgroups.JChannel;

/**
 * Description of a transport or protocol resource.
 * @author Paul Ferraro
 */
public interface ProtocolChildResourceDescription extends ResourceDescription {
    static final ModuleAttributeDefinition MODULE = new ModuleAttributeDefinition.Builder().setDefaultValue(Module.forClass(JChannel.class)).build();
    static final PropertiesAttributeDefinition PROPERTIES = new PropertiesAttributeDefinition.Builder().build();
    static final StatisticsEnabledAttributeDefinition STATISTICS_ENABLED = new StatisticsEnabledAttributeDefinition.Builder().setDefaultValue(null).build();

    @Override
    default Stream<AttributeDefinition> getAttributes() {
        return Stream.of(MODULE, STATISTICS_ENABLED, PROPERTIES);
    }
}
