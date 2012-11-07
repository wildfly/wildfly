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

package org.jboss.as.connector.subsystems.jca;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import static org.jboss.as.connector.subsystems.jca.Constants.CACHED_CONNECTION_MANAGER;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class JcaCachedConnectionManagerDefinition extends SimpleResourceDefinition {
    protected static final PathElement PATH_CACHED_CONNECTION_MANAGER = PathElement.pathElement(CACHED_CONNECTION_MANAGER, CACHED_CONNECTION_MANAGER);
    static final JcaCachedConnectionManagerDefinition INSTANCE = new JcaCachedConnectionManagerDefinition();

    private JcaCachedConnectionManagerDefinition() {
        super(PATH_CACHED_CONNECTION_MANAGER,
                JcaExtension.getResourceDescriptionResolver(PATH_CACHED_CONNECTION_MANAGER.getKey()),
                CachedConnectionManagerAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        for (final CcmParameters parameter : CcmParameters.values()) {
            if (parameter != CcmParameters.INSTALL) {
                resourceRegistration.registerReadWriteAttribute(parameter.getAttribute(), null, JcaAttributeWriteHandler.INSTANCE);
            } else {
                resourceRegistration.registerReadWriteAttribute(parameter.getAttribute(), null, new ReloadRequiredWriteAttributeHandler());
            }
        }

    }

    public static enum CcmParameters {
        DEBUG(SimpleAttributeDefinitionBuilder.create("debug", ModelType.BOOLEAN)
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set(false))
                .setMeasurementUnit(MeasurementUnit.NONE)
                .setRestartAllServices()
                .setXmlName("debug")
                .build()),
        ERROR(SimpleAttributeDefinitionBuilder.create("error", ModelType.BOOLEAN)
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set(false))
                .setMeasurementUnit(MeasurementUnit.NONE)
                .setRestartAllServices()
                .setXmlName("error")
                .build()),
        INSTALL(SimpleAttributeDefinitionBuilder.create("install", ModelType.BOOLEAN)
                .setAllowExpression(false)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set(false))
                .setMeasurementUnit(MeasurementUnit.NONE)
                .setRestartAllServices()
                .build());


        private CcmParameters(SimpleAttributeDefinition attribute) {
            this.attribute = attribute;
        }

        public SimpleAttributeDefinition getAttribute() {
            return attribute;
        }

        private SimpleAttributeDefinition attribute;
    }


}
