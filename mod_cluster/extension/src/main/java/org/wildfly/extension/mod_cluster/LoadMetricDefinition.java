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
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modcluster.load.metric.LoadMetric;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class LoadMetricDefinition extends SimpleResourceDefinition {

    static final PathElement PATH = PathElement.pathElement(CommonAttributes.LOAD_METRIC);

    protected static final LoadMetricDefinition INSTANCE = new LoadMetricDefinition();

    static final SimpleAttributeDefinition TYPE = SimpleAttributeDefinitionBuilder.create(CommonAttributes.TYPE, ModelType.STRING, false)
            .setAllowExpression(false)
            .setRestartAllServices()
            .setValidator(new EnumValidator<LoadMetricEnum>(LoadMetricEnum.class, false, false))
            .build();

    static final SimpleAttributeDefinition WEIGHT = SimpleAttributeDefinitionBuilder.create(CommonAttributes.WEIGHT, ModelType.INT, true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .setDefaultValue(new ModelNode(LoadMetric.DEFAULT_WEIGHT))
            .build();

    static final SimpleAttributeDefinition CAPACITY = SimpleAttributeDefinitionBuilder.create(CommonAttributes.CAPACITY, ModelType.DOUBLE, true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .setDefaultValue(new ModelNode(LoadMetric.DEFAULT_CAPACITY))
            .build();

    static final PropertiesAttributeDefinition PROPERTY = new PropertiesAttributeDefinition.Builder(CommonAttributes.PROPERTY, true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    static final AttributeDefinition[] ATTRIBUTES = {
            TYPE, WEIGHT, CAPACITY, PROPERTY
    };

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder builder) {
        // Nothing to transform since 1.4.0
    }

    private LoadMetricDefinition() {
        super(PATH,
                ModClusterExtension.getResourceDescriptionResolver(CommonAttributes.CONFIGURATION, CommonAttributes.DYNAMIC_LOAD_PROVIDER, CommonAttributes.LOAD_METRIC),
                LoadMetricAdd.INSTANCE,
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
