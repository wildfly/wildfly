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

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.Consumer;

import org.jboss.as.clustering.controller.transform.DiscardSingletonListAttributeChecker;
import org.jboss.as.clustering.controller.transform.RejectNonSingletonListAttributeChecker;
import org.jboss.as.clustering.controller.transform.SingletonListAttributeConverter;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Transformer for cache container resources.
 * @author Paul Ferraro
 */
public class CacheContainerResourceTransformer implements Consumer<ModelVersion> {

    private final ResourceTransformationDescriptionBuilder builder;

    CacheContainerResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        this.builder = parent.addChildResource(CacheContainerResourceDefinition.WILDCARD_PATH);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void accept(ModelVersion version) {
        if (InfinispanModel.VERSION_15_0_0.requiresTransformation(version)) {
            this.builder.getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, CacheContainerResourceDefinition.Attribute.MARSHALLER.getDefinition())
                    .addRejectCheck(new RejectAttributeChecker.SimpleAcceptAttributeChecker(CacheContainerResourceDefinition.Attribute.MARSHALLER.getDefinition().getDefaultValue()), CacheContainerResourceDefinition.Attribute.MARSHALLER.getDefinition())
                    .end();
        }
        if (InfinispanModel.VERSION_14_0_0.requiresTransformation(version)) {
            this.builder.getAttributeBuilder()
                    .setValueConverter(new SingletonListAttributeConverter(CacheContainerResourceDefinition.ListAttribute.MODULES), CacheContainerResourceDefinition.DeprecatedAttribute.MODULE.getDefinition())
                    .setDiscard(DiscardSingletonListAttributeChecker.INSTANCE, CacheContainerResourceDefinition.ListAttribute.MODULES.getDefinition())
                    .addRejectCheck(RejectNonSingletonListAttributeChecker.INSTANCE, CacheContainerResourceDefinition.ListAttribute.MODULES.getDefinition())
                    .end();
        }

        new ScatteredCacheResourceTransformer(this.builder).accept(version);
        new DistributedCacheResourceTransformer(this.builder).accept(version);
        new ReplicatedCacheResourceTransformer(this.builder).accept(version);
        new InvalidationCacheResourceTransformer(this.builder).accept(version);
        new LocalCacheResourceTransformer(this.builder).accept(version);
    }
}
