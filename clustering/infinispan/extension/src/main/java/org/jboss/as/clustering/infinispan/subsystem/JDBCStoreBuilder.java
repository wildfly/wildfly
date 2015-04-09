/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.clustering.infinispan.subsystem.JDBCStoreResourceDefinition.Attribute.DATA_SOURCE;
import static org.jboss.as.clustering.infinispan.subsystem.JDBCStoreResourceDefinition.Attribute.DIALECT;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.persistence.jdbc.DatabaseType;
import org.infinispan.persistence.jdbc.configuration.AbstractJdbcStoreConfiguration;
import org.infinispan.persistence.jdbc.configuration.AbstractJdbcStoreConfigurationBuilder;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author Paul Ferraro
 */
public abstract class JDBCStoreBuilder<C extends AbstractJdbcStoreConfiguration, B extends AbstractJdbcStoreConfigurationBuilder<C, B>> extends StoreBuilder {

    private final Class<B> builderClass;

    private volatile String dataSource;

    JDBCStoreBuilder(Class<B> builderClass, String containerName, String cacheName) {
        super(containerName, cacheName);
        this.builderClass = builderClass;
    }

    @Override
    public ServiceBuilder<PersistenceConfiguration> build(ServiceTarget target) {
        return super.build(target).addDependency(ServiceName.JBOSS.append("data-source", this.dataSource));
    }

    @Override
    B createStore(ExpressionResolver resolver, ModelNode model) throws OperationFailedException {
        this.dataSource = DATA_SOURCE.getDefinition().resolveModelAttribute(resolver, model).asString();
        B storeBuilder = new ConfigurationBuilder().persistence().addStore(this.builderClass).dialect(ModelNodes.asEnum(DIALECT.getDefinition().resolveModelAttribute(resolver, model), DatabaseType.class));
        storeBuilder.dataSource().jndiUrl(this.dataSource);
        return storeBuilder;
    }
}
