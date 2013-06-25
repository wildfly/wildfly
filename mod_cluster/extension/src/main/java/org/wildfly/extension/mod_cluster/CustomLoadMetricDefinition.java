/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.mod_cluster;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class CustomLoadMetricDefinition extends SimpleResourceDefinition {

    protected static final CustomLoadMetricDefinition INSTANCE = new CustomLoadMetricDefinition();

    static final SimpleAttributeDefinition CLASS = SimpleAttributeDefinitionBuilder.create(CommonAttributes.CLASS, ModelType.STRING, false)
            .setAllowExpression(false)
            .setRestartAllServices()
            .build();


    static final AttributeDefinition[] ATTRIBUTES = {
            CLASS, LoadMetricDefinition.WEIGHT, LoadMetricDefinition.CAPACITY, LoadMetricDefinition.PROPERTY
    };

    private CustomLoadMetricDefinition() {
        super(ModClusterExtension.CUSTOM_LOAD_METRIC_PATH,
                ModClusterExtension.getResourceDescriptionResolver(CommonAttributes.CONFIGURATION, CommonAttributes.DYNAMIC_LOAD_PROVIDER, CommonAttributes.LOAD_METRIC),
                CustomLoadMetricAdd.INSTANCE,
                new ReloadRequiredRemoveStepHandler()
        );
    }


    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition def : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(def, null, new ReloadRequiredWriteAttributeHandler(def));
        }
    }

}
