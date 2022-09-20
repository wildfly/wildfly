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

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Transformers for cache resources.
 * @author Paul Ferraro
 */
public class CacheResourceTransformer implements Consumer<ModelVersion> {

    final ResourceTransformationDescriptionBuilder builder;

    CacheResourceTransformer(ResourceTransformationDescriptionBuilder builder) {
        this.builder = builder;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void accept(ModelVersion version) {
        new TransactionResourceTransformer(this.builder).accept(version);
        new CustomStoreResourceTransformer(this.builder).accept(version);
        new FileStoreResourceTransformer(this.builder).accept(version);
        new HotRodStoreResourceTransformer(this.builder).accept(version);
        new JDBCStoreResourceTransformer(this.builder).accept(version);
        new RemoteStoreResourceTransformer(this.builder).accept(version);
    }
}
