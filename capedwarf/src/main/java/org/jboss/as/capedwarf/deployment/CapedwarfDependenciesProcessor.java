/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.capedwarf.deployment;

import org.jboss.as.capedwarf.api.Constants;
import org.jboss.as.capedwarf.services.IndexingConsumerService;
import org.jboss.as.capedwarf.services.ServletExecutorConsumerService;
import org.jboss.as.clustering.infinispan.subsystem.CacheConfigurationService;
import org.jboss.as.logging.util.LogServices;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Define any MSC dependencies.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CapedwarfDependenciesProcessor extends CapedwarfDeploymentUnitProcessor {

    private static final ServiceName[] CACHE_CONFIGS;

    static {
        CACHE_CONFIGS = new ServiceName[Constants.CACHES.length];
        for (int i = 0; i < CACHE_CONFIGS.length; i++) {
            CACHE_CONFIGS[i] = CacheConfigurationService.getServiceName(CAPEDWARF, Constants.CACHES[i]);
        }
    }

    protected void doDeploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        // make sure the cache configs are registerd into container before we get the cache
        for (ServiceName cc : CACHE_CONFIGS) {
            serviceTarget.addDependency(cc);
        }
        serviceTarget.addDependency(ServletExecutorConsumerService.NAME); // we need queue -- as default gae queue is there by default
        serviceTarget.addDependency(IndexingConsumerService.NAME); // we need indexing
        serviceTarget.addDependency(LogServices.loggerHandlerName("ROOT", CAPEDWARF.toUpperCase())); // we need logger
    }

}
