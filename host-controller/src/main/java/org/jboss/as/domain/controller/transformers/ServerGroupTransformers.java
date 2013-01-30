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

package org.jboss.as.domain.controller.transformers;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.as.domain.controller.resources.ServerGroupResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Transformer registration for the server-group resources.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
class ServerGroupTransformers {

    static void registerTransformers120(TransformersSubRegistration parent) {
        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createInstance(ServerGroupResourceDefinition.PATH)
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, ServerGroupResourceDefinition.MANAGEMENT_SUBSYSTEM_ENDPOINT, ServerGroupResourceDefinition.SOCKET_BINDING_PORT_OFFSET)
                .setValueConverter(new AttributeConverter.DefaultAttributeConverter() {
                    @Override
                    protected void convertAttribute(PathAddress address, String attributeName, ModelNode attributeValue,
                            TransformationContext context) {
                        //7.1.x had a strict type for the management-susbsystem-endpoint attribute, convert that here if it comes in as a string
                        if (attributeValue.isDefined() && attributeValue.getType() == ModelType.STRING) {
                            attributeValue.set(attributeValue.asBoolean());
                        }
                    }
                }, ServerGroupResourceDefinition.MANAGEMENT_SUBSYSTEM_ENDPOINT)
                .end();
        TransformersSubRegistration serverGroup = TransformationDescription.Tools.register(builder.build(), parent);

        DeploymentTransformers.registerTransformers120(serverGroup);

        SystemPropertyTransformers.registerTransformers120(serverGroup);
        JvmTransformers.registerTransformers120(serverGroup);
    }
}
