/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.Consumer;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Transformers for cache store resources.
 * @author Paul Ferraro
 */
public class StoreResourceTransformer implements Consumer<ModelVersion> {

    final ResourceTransformationDescriptionBuilder builder;

    StoreResourceTransformer(ResourceTransformationDescriptionBuilder builder) {
        this.builder = builder;
    }

    @Override
    public void accept(ModelVersion version) {
        if (InfinispanModel.VERSION_16_0_0.requiresTransformation(version)) {
            this.builder.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, StoreResourceDefinition.Attribute.SEGMENTED.getDefinition())
                .addRejectCheck(RejectAttributeChecker.DEFINED, StoreResourceDefinition.Attribute.SEGMENTED.getDefinition())
                .setValueConverter(AttributeConverter.DEFAULT_VALUE, StoreResourceDefinition.Attribute.PASSIVATION.getDefinition())
                .setValueConverter(AttributeConverter.DEFAULT_VALUE, StoreResourceDefinition.Attribute.PURGE.getDefinition())
                .end();
        }
    }
}
