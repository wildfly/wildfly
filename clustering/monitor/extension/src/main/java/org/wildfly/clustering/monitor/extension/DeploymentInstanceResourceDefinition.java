/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.monitor.extension;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * Resource representing a run-time only cluster deployment instance.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class DeploymentInstanceResourceDefinition extends SimpleResourceDefinition {

    public static final PathElement DEPLOYMENT_PATH = PathElement.pathElement(ModelKeys.DEPLOYMENT);
    private final boolean runtimeRegistration;

    // metrics
    static final SimpleAttributeDefinition MODULE_IDENTIFIER =
            new SimpleAttributeDefinitionBuilder(ModelKeys.MODULE_IDENTIFIER, ModelType.STRING, false)
                    .setStorageRuntime()
                    .build();

    static final AttributeDefinition[] DEPLOYMENT_METRICS = {MODULE_IDENTIFIER};

    public DeploymentInstanceResourceDefinition(boolean runtimeRegistration) {

        super(DEPLOYMENT_PATH,
                ClusteringMonitorExtension.getResourceDescriptionResolver(ModelKeys.CLUSTER + "." + ModelKeys.DEPLOYMENT),
                null,
                null);
        this.runtimeRegistration = runtimeRegistration;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        // register any metrics and the read-only handler
        if (runtimeRegistration) {
            for (AttributeDefinition attr : DEPLOYMENT_METRICS) {
                resourceRegistration.registerMetric(attr, DeploymentMetricsHandler.INSTANCE);
            }
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new WebInstanceResourceDefinition(true));
        resourceRegistration.registerSubModel(new BeanInstanceResourceDefinition(true));
    }

}
