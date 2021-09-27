/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.function.Consumer;

import org.jboss.as.clustering.controller.transform.SimpleAttributeConverter;
import org.jboss.as.clustering.infinispan.subsystem.InfinispanModel;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;

/**
 * @author Paul Ferraro
 */
public class RemoteTransactionResourceTransformer implements Consumer<ModelVersion> {

    private final ResourceTransformationDescriptionBuilder builder;

    @SuppressWarnings("deprecation")
    RemoteTransactionResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        this.builder = parent.addChildResource(RemoteTransactionResourceDefinition.PATH);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void accept(ModelVersion version) {
        if (InfinispanModel.VERSION_15_0_0.requiresTransformation(version)) {
            AttributeConverter converter = new SimpleAttributeConverter(new SimpleAttributeConverter.Converter() {
                @Override
                public void convert(PathAddress address, String name, ModelNode value, ModelNode model, TransformationContext context) {
                    ModelNode parentModel = context.readResourceFromRoot(address.getParent()).getModel();
                    if (parentModel.hasDefined(RemoteCacheContainerResourceDefinition.Attribute.TRANSACTION_TIMEOUT.getName())) {
                        value.set(parentModel.get(RemoteCacheContainerResourceDefinition.Attribute.TRANSACTION_TIMEOUT.getName()));
                    }
                }
            });
            this.builder.getAttributeBuilder().setValueConverter(converter, RemoteTransactionResourceDefinition.Attribute.TIMEOUT.getDefinition());
        }
    }
}
