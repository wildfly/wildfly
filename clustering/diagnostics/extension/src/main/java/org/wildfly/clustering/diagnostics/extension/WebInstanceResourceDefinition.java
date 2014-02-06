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
package org.wildfly.clustering.diagnostics.extension;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * Resource representing a run-time only clustered web app instance.
 * <p/>
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class WebInstanceResourceDefinition extends SimpleResourceDefinition {

    public static final PathElement WEB_PATH = PathElement.pathElement(ModelKeys.WEB, ModelKeys.WEB_NAME);
    private final boolean runtimeRegistration;

    // metrics
    static final AttributeDefinition NODE_NAME = new SimpleAttributeDefinitionBuilder(ModelKeys.NODE_NAME, ModelType.STRING)
                     .setAllowNull(false)
                     .build();
    static final AttributeDefinition NODE_VALUE = new SimpleAttributeDefinitionBuilder(ModelKeys.NODE_VALUE, ModelType.STRING)
                     .setAllowNull(false)
                     .build();

    static final ObjectTypeAttributeDefinition NODE_RESULT = ObjectTypeAttributeDefinition.Builder.of(ModelKeys.NODE_RESULT, NODE_NAME, NODE_VALUE)
                     .setAllowNull(false)
                     .setSuffix("node-result")
                     .build();

    static final ObjectListAttributeDefinition CACHE_VIEW = ObjectListAttributeDefinition.Builder.of(ModelKeys.CACHE_VIEW, NODE_RESULT)
                    .setStorageRuntime()
                    .build();

    // not yet implemented in ISPN
    static final ObjectListAttributeDefinition CACHE_VIEW_HISTORY = ObjectListAttributeDefinition.Builder.of(ModelKeys.CACHE_VIEW_HISTORY, NODE_RESULT)
                    .setStorageRuntime()
                    .build();

    static final ObjectListAttributeDefinition DISTRIBUTION = ObjectListAttributeDefinition.Builder.of(ModelKeys.DISTRIBUTION, NODE_RESULT)
                    .setStorageRuntime()
                    .build();

    static final ObjectListAttributeDefinition OPERATION_STATS = ObjectListAttributeDefinition.Builder.of(ModelKeys.OPERATION_STATS, NODE_RESULT)
                    .setStorageRuntime()
                    .build();

    static final ObjectListAttributeDefinition RPC_STATS = ObjectListAttributeDefinition.Builder.of(ModelKeys.RPC_STATS, NODE_RESULT)
                    .setStorageRuntime()
                    .build();

    static final ObjectListAttributeDefinition TXN_STATS = ObjectListAttributeDefinition.Builder.of(ModelKeys.TXN_STATS, NODE_RESULT)
                    .setStorageRuntime()
                    .build();

    static final AttributeDefinition[] WEB_METRICS = {CACHE_VIEW, DISTRIBUTION, OPERATION_STATS, RPC_STATS, TXN_STATS};

    public WebInstanceResourceDefinition(boolean runtimeRegistration) {

        super(WEB_PATH,
                ClusteringDiagnosticsExtension.getResourceDescriptionResolver(ModelKeys.CLUSTER + "." + ModelKeys.DEPLOYMENT + "." + ModelKeys.WEB),
                null,
                null);
        this.runtimeRegistration = runtimeRegistration;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        // register any metrics and the read-only handler
        if (runtimeRegistration) {
            for (AttributeDefinition attr : WEB_METRICS) {
                resourceRegistration.registerMetric(attr, WebMetricsHandler.INSTANCE);
            }
        }
    }
}
