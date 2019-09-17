/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.clustering.controller.AttributeTranslation;
import org.jboss.as.clustering.controller.ReadAttributeTranslationHandler;
import org.jboss.as.clustering.controller.Registration;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Enumeration of management metrics for cache eviction
 * @author Paul Ferraro
 */
public enum EvictionMetric implements AttributeTranslation, Registration<ManagementResourceRegistration>, UnaryOperator<PathAddress> {

    EVICTIONS(CacheMetric.EVICTIONS),
    ;
    private final AttributeDefinition definition;
    private final Attribute targetAttribute;

    EvictionMetric(Attribute targetAttribute) {
        this.definition = new SimpleAttributeDefinitionBuilder(targetAttribute.getName(), targetAttribute.getDefinition().getType())
                .setDeprecated(InfinispanModel.VERSION_11_0_0.getVersion())
                .setStorageRuntime()
                .build();
        this.targetAttribute = targetAttribute;
    }

    @Override
    public Attribute getTargetAttribute() {
        return this.targetAttribute;
    }

    @Override
    public UnaryOperator<PathAddress> getPathAddressTransformation() {
        return this;
    }

    @Override
    public PathAddress apply(PathAddress address) {
        PathAddress cacheAddress = address.getParent();
        return cacheAddress.getParent().append(CacheRuntimeResourceDefinition.pathElement(cacheAddress.getLastElement().getValue()));
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        registration.registerReadOnlyAttribute(this.definition, new ReadAttributeTranslationHandler(this));
    }
}