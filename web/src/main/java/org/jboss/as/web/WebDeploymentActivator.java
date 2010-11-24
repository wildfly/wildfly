/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.web;

import org.jboss.as.deployment.Phase;
import org.jboss.as.deployment.chain.DeploymentChain;
import org.jboss.as.deployment.chain.DeploymentChainProcessorInjector;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitProcessorService;
import org.jboss.as.web.deployment.JBossWebParsingDeploymentProcessor;
import org.jboss.as.web.deployment.ServletContainerInitializerDeploymentProcessor;
import org.jboss.as.web.deployment.TldParsingDeploymentProcessor;
import org.jboss.as.web.deployment.WarAnnotationDeploymentProcessor;
import org.jboss.as.web.deployment.WarAnnotationIndexProcessor;
import org.jboss.as.web.deployment.WarClassloadingDependencyProcessor;
import org.jboss.as.web.deployment.WarDeploymentInitializingProcessor;
import org.jboss.as.web.deployment.WarDeploymentProcessor;
import org.jboss.as.web.deployment.WarMetaDataProcessor;
import org.jboss.as.web.deployment.WarModuleConfigProcessor;
import org.jboss.as.web.deployment.WarStructureDeploymentProcessor;
import org.jboss.as.web.deployment.WebFragmentParsingDeploymentProcessor;
import org.jboss.as.web.deployment.WebParsingDeploymentProcessor;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;

/**
 * @author Emanuel Muckenhuber
 */
class WebDeploymentActivator {

    static void activate(final String defaultHost, final SharedWebMetaDataBuilder sharedWebBuilder, final SharedTldsMetaDataBuilder sharedTldsBuilder, final BatchBuilder batchBuilder) {
        // Web specific deployment processors ....
        addDeploymentProcessor(batchBuilder, new WarDeploymentInitializingProcessor(), Phase.WAR_DEPLOYMENT_INITIALIZING_PROCESSOR);
        addDeploymentProcessor(batchBuilder, new WarStructureDeploymentProcessor(sharedWebBuilder.create(), sharedTldsBuilder.create()), Phase.WAR_STRUCTURE_DEPLOYMENT_PROCESSOR);
        addDeploymentProcessor(batchBuilder, new WarAnnotationIndexProcessor(), Phase.WAR_ANNOTATION_INDEX_PROCESSOR);
        addDeploymentProcessor(batchBuilder, new WarModuleConfigProcessor(), Phase.WAR_MODULE_CONFIG_PROCESSOR);
        addDeploymentProcessor(batchBuilder, new WebParsingDeploymentProcessor(), Phase.WEB_PARSING_DEPLOYMENT_PROCESSOR);
        addDeploymentProcessor(batchBuilder, new WebFragmentParsingDeploymentProcessor(), Phase.WEB_FRAGMENT_PARSING_DEPLOYMENT_PROCESSOR);
        addDeploymentProcessor(batchBuilder, new JBossWebParsingDeploymentProcessor(), Phase.JBOSS_WEB_PARSING_DEPLOYMENT_PROCESSOR);
        addDeploymentProcessor(batchBuilder, new TldParsingDeploymentProcessor(), Phase.TLD_PARSING_DEPLOYMENT_PROCESSOR);
        addDeploymentProcessor(batchBuilder, new WarClassloadingDependencyProcessor(), Phase.WAR_CLASSLOADING_DEPENDENCY_PROCESSOR);
        addDeploymentProcessor(batchBuilder, new WarAnnotationDeploymentProcessor(), Phase.WAR_ANNOTATION_DEPLOYMENT_PROCESSOR);
        addDeploymentProcessor(batchBuilder, new ServletContainerInitializerDeploymentProcessor(), Phase.SERVLET_CONTAINER_INITIALIZER_DEPLOYMENT_PROCESSOR);
        addDeploymentProcessor(batchBuilder, new WarMetaDataProcessor(), Phase.WAR_META_DATA_PROCESSOR);
        addDeploymentProcessor(batchBuilder, new WarDeploymentProcessor(defaultHost), Phase.WAR_DEPLOYMENT_PROCESSOR);

    }

    static <T extends DeploymentUnitProcessor> BatchServiceBuilder<T> addDeploymentProcessor(final BatchBuilder batchBuilder, final T deploymentUnitProcessor, final long priority) {
        final DeploymentUnitProcessorService<T> deploymentUnitProcessorService = new DeploymentUnitProcessorService<T>(deploymentUnitProcessor);
        return batchBuilder.addService(DeploymentUnitProcessor.SERVICE_NAME_BASE.append(deploymentUnitProcessor.getClass().getName()), deploymentUnitProcessorService)
            .addDependency(DeploymentChain.SERVICE_NAME, DeploymentChain.class, new DeploymentChainProcessorInjector<T>(deploymentUnitProcessorService, priority));
    }

}
