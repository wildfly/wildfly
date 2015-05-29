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

package org.jboss.as.jpa.processor.secondLevelCache;

import java.util.HashMap;
import java.util.Properties;

import org.jboss.msc.service.ServiceBuilder;
import org.jipijapa.cache.spi.Classification;
import org.jipijapa.cache.spi.Wrapper;
import org.jipijapa.event.spi.EventListener;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * CacheDeploymentListener
 *
 * @author Scott Marlow
 */
public class CacheDeploymentListener implements EventListener {

    private static final ThreadLocal<ServiceBuilder<?>> SERVICEBUILDER_TLS = new ThreadLocal<>();

    HashMap<String,EventListener> delegates = new HashMap<String,EventListener>();

    public CacheDeploymentListener() {
        delegates.put(Classification.INFINISPAN.getLocalName(), new InfinispanCacheDeploymentListener());
    }

    public static void setInternalDeploymentServiceBuilder(ServiceBuilder<?> serviceBuilder) {
        SERVICEBUILDER_TLS.set(serviceBuilder);
    }

    public static void clearInternalDeploymentServiceBuilder() {
        SERVICEBUILDER_TLS.remove();
    }

    public static ServiceBuilder<?> getInternalDeploymentServiceBuilder() {
        return SERVICEBUILDER_TLS.get();
    }


    @Override
    public void beforeEntityManagerFactoryCreate(Classification classification, PersistenceUnitMetadata persistenceUnitMetadata) {

        delegates.get(classification.getLocalName()).beforeEntityManagerFactoryCreate(classification, persistenceUnitMetadata);
    }

    @Override
    public void afterEntityManagerFactoryCreate(Classification classification, PersistenceUnitMetadata persistenceUnitMetadata) {
        SERVICEBUILDER_TLS.remove();
        delegates.get(classification.getLocalName()).afterEntityManagerFactoryCreate(classification, persistenceUnitMetadata);
    }

    @Override
    public Wrapper startCache(Classification classification, Properties properties) throws Exception {
        return delegates.get(classification.getLocalName()).startCache(classification, properties);
    }

    @Override
    public void addCacheDependencies(Classification classification, Properties properties) {
        delegates.get(classification.getLocalName()).addCacheDependencies(classification, properties);
    }

    @Override
    public void stopCache(Classification classification, Wrapper wrapper, boolean skipStop) {
        delegates.get(classification.getLocalName()).stopCache(classification, wrapper, skipStop);
    }
}
