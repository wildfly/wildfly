/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ejb.infinispan.bean;

import java.util.function.Function;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.ejb.bean.BeanManagementProvider;
import org.wildfly.clustering.ejb.bean.BeanDeploymentMarshallingContext;
import org.wildfly.clustering.ejb.bean.LegacyBeanManagementConfiguration;
import org.wildfly.clustering.ejb.bean.LegacyBeanManagementProviderFactory;
import org.wildfly.clustering.ejb.cache.bean.BeanMarshallerFactory;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;

/**
 * A {@link LegacyBeanManagementProviderFactory} implementation that creates a suitable {@link BeanManagementProvider} when no distributable-ejb subsystem is present.
 * @author Paul Ferraro
 * @deprecated This exists to support legacy configuration that does not define a distributable-ejb subsystem.
 */
@Deprecated
@MetaInfServices(LegacyBeanManagementProviderFactory.class)
public class InfinispanLegacyBeanManagementProviderFactory implements LegacyBeanManagementProviderFactory {

    @Override
    public BeanManagementProvider createBeanManagementProvider(String name, LegacyBeanManagementConfiguration config) {
        return new InfinispanBeanManagementProvider(name, new InfinispanBeanManagementConfiguration() {
            @Override
            public String getContainerName() {
                return config.getContainerName();
            }

            @Override
            public String getCacheName() {
                return config.getCacheName();
            }

            @Override
            public Integer getMaxActiveBeans() {
                return config.getMaxActiveBeans();
            }

            @Override
            public Function<BeanDeploymentMarshallingContext, ByteBufferMarshaller> getMarshallerFactory() {
                return BeanMarshallerFactory.JBOSS;
            }
        });
    }
}
