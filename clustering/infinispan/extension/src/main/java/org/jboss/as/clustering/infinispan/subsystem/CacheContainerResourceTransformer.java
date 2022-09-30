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

import java.util.Set;
import java.util.function.Consumer;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.Attribute;
import org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.ListAttribute;
import org.jboss.dmr.ModelNode;

/**
 * Transformer for cache container resources.
 * @author Paul Ferraro
 */
public class CacheContainerResourceTransformer implements Consumer<ModelVersion> {

    private static final Set<String> SERVER_MODULES = Set.of("org.wildfly.clustering.server.infinispan", "org.wildfly.clustering.singleton.server");
    private static final String LEGACY_SERVER_MODULE = "org.wildfly.clustering.server";

    private final ResourceTransformationDescriptionBuilder builder;

    CacheContainerResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        this.builder = parent.addChildResource(CacheContainerResourceDefinition.WILDCARD_PATH);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void accept(ModelVersion version) {
        if (InfinispanModel.VERSION_16_0_0.requiresTransformation(version)) {
            this.builder.getAttributeBuilder()
                    .setValueConverter(new AttributeConverter.DefaultAttributeConverter() {
                        @Override
                        protected void convertAttribute(PathAddress address, String name, ModelNode modules, TransformationContext context) {
                            if (modules.isDefined()) {
                                // Handle refactoring of org.wildfly.clustering.server module
                                for (ModelNode module : modules.asList()) {
                                    if (SERVER_MODULES.contains(module.asString())) {
                                        module.set(LEGACY_SERVER_MODULE);
                                    }
                                }
                            }
                        }
                    }, ListAttribute.MODULES.getDefinition())
                    .end();
        }
        if (InfinispanModel.VERSION_15_0_0.requiresTransformation(version)) {
            this.builder.getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, Attribute.MARSHALLER.getDefinition())
                    .addRejectCheck(new RejectAttributeChecker.SimpleAcceptAttributeChecker(Attribute.MARSHALLER.getDefinition().getDefaultValue()), Attribute.MARSHALLER.getDefinition())
                    .end();
        }

        new ScatteredCacheResourceTransformer(this.builder).accept(version);
        new DistributedCacheResourceTransformer(this.builder).accept(version);
        new ReplicatedCacheResourceTransformer(this.builder).accept(version);
        new InvalidationCacheResourceTransformer(this.builder).accept(version);
        new LocalCacheResourceTransformer(this.builder).accept(version);
    }
}
