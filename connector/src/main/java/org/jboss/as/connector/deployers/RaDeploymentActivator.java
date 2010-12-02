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

package org.jboss.as.connector.deployers;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.deployers.processors.DsDependencyProcessor;
import org.jboss.as.connector.deployers.processors.DsDeploymentProcessor;
import org.jboss.as.connector.deployers.processors.IronJacamarDeploymentParsingProcessor;
import org.jboss.as.connector.deployers.processors.ParsedRaDeploymentProcessor;
import org.jboss.as.connector.deployers.processors.RaDeploymentParsingProcessor;
import org.jboss.as.connector.deployers.processors.RaNestedJarInlineProcessor;
import org.jboss.as.connector.deployers.processors.RaXmlDeploymentProcessor;
import org.jboss.as.connector.deployers.processors.RarConfigProcessor;
import org.jboss.as.connector.jndi.JndiStrategyService;
import org.jboss.as.connector.mdr.MdrService;
import org.jboss.as.connector.registry.ResourceAdapterDeploymentRegistryService;
import org.jboss.as.deployment.Phase;
import org.jboss.as.model.BootUpdateContext;
import org.jboss.msc.service.ServiceTarget;

/**
 * Service activator which installs the various service required for rar
 * deployments.
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 */
public class RaDeploymentActivator {
    /**
     * Activate the services required for service deployments.
     * @param updateContext The update context
     */
    public void activate(final BootUpdateContext updateContext) {
        final ServiceTarget serviceTarget = updateContext.getBatchBuilder();

        // add resources here
        MdrService mdrService = new MdrService();
        serviceTarget.addService(ConnectorServices.IRONJACAMAR_MDR, mdrService).install();

        ResourceAdapterDeploymentRegistryService registryService = new ResourceAdapterDeploymentRegistryService();
        serviceTarget.addService(ConnectorServices.RESOURCE_ADAPTER_REGISTRY_SERVICE, registryService).install();

        JndiStrategyService jndiStrategyService = new JndiStrategyService();
        serviceTarget.addService(ConnectorServices.JNDI_STRATEGY_SERVICE, jndiStrategyService).install();

        updateContext.addDeploymentProcessor(new RarConfigProcessor(), Phase.RAR_CONFIG_PROCESSOR);
        updateContext.addDeploymentProcessor(new RaDeploymentParsingProcessor(), Phase.RA_DEPLOYMENT_PARSING_PROCESSOR);
        updateContext.addDeploymentProcessor(new IronJacamarDeploymentParsingProcessor(), Phase.IRON_JACAMAR_DEPLOYMENT_PARSING_PROCESSOR);
        updateContext.addDeploymentProcessor(new ParsedRaDeploymentProcessor(), Phase.PARSED_RA_DEPLOYMENT_PROCESSOR);
        updateContext.addDeploymentProcessor(new RaXmlDeploymentProcessor(mdrService.getValue()), Phase.RA_XML_DEPLOYMENT_PROCESSOR);
        updateContext.addDeploymentProcessor(new DsDependencyProcessor(), Phase.DS_DEPENDENCY_PROCESSOR);
        updateContext.addDeploymentProcessor(new DsDeploymentProcessor(), Phase.DS_DEPLOYMENT_PROCESSOR);
        updateContext.addDeploymentProcessor(new RaNestedJarInlineProcessor(), Phase.RA_NESTED_JAR_INLINE_PROCESSOR);
    }
}
