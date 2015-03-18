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

package org.wildfly.clustering.server.provider;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.wildfly.clustering.provider.ServiceProviderRegistrationFactory;
import org.wildfly.clustering.service.AliasServiceBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.CacheGroupServiceName;
import org.wildfly.clustering.spi.CacheGroupAliasBuilderProvider;
import org.wildfly.clustering.spi.GroupServiceName;

/**
 * @author Paul Ferraro
 */
public class ServiceProviderRegistrationFactoryAliasBuilderProvider implements CacheGroupAliasBuilderProvider {

    @Override
    public Collection<Builder<?>> getBuilders(String containerName, String aliasCacheName, String targetCacheName) {
        Builder<ServiceProviderRegistrationFactory> builder = new AliasServiceBuilder<>(CacheGroupServiceName.SERVICE_PROVIDER_REGISTRATION.getServiceName(containerName, aliasCacheName), CacheGroupServiceName.SERVICE_PROVIDER_REGISTRATION.getServiceName(containerName, targetCacheName), ServiceProviderRegistrationFactory.class);
        ContextNames.BindInfo binding = ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, GroupServiceName.BASE_NAME, CacheGroupServiceName.SERVICE_PROVIDER_REGISTRATION.toString(), containerName, aliasCacheName).getAbsoluteName());
        Builder<ManagedReferenceFactory> bindingBuilder = new BinderServiceBuilder<>(binding, builder.getServiceName(), ServiceProviderRegistrationFactory.class);
        return Arrays.asList(builder, bindingBuilder);
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }
}
