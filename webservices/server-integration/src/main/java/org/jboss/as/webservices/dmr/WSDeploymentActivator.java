/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.webservices.dmr;

import java.util.List;

import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.webservices.deployers.AspectDeploymentProcessor;
import org.jboss.as.webservices.deployers.GracefulShutdownIntegrationProcessor;
import org.jboss.as.webservices.deployers.JBossWebservicesDescriptorDeploymentProcessor;
import org.jboss.as.webservices.deployers.WSClassVerificationProcessor;
import org.jboss.as.webservices.deployers.WSDependenciesProcessor;
import org.jboss.as.webservices.deployers.WSIntegrationProcessorJAXWS_EJB;
import org.jboss.as.webservices.deployers.WSIntegrationProcessorJAXWS_HANDLER;
import org.jboss.as.webservices.deployers.WSIntegrationProcessorJAXWS_JMS;
import org.jboss.as.webservices.deployers.WSIntegrationProcessorJAXWS_POJO;
import org.jboss.as.webservices.deployers.WSLibraryFilterProcessor;
import org.jboss.as.webservices.deployers.WSModelDeploymentProcessor;
import org.jboss.as.webservices.deployers.WSServiceDependenciesProcessor;
import org.jboss.as.webservices.deployers.WebServiceAnnotationProcessor;
import org.jboss.as.webservices.deployers.WebServicesContextJndiSetupProcessor;
import org.jboss.as.webservices.deployers.WebservicesDescriptorDeploymentProcessor;
import org.jboss.as.webservices.deployers.deployment.DeploymentAspectsProvider;
import org.jboss.as.webservices.injection.WSHandlerChainAnnotationProcessor;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.as.webservices.util.ModuleClassLoaderProvider;
import org.jboss.as.webservices.webserviceref.WSRefAnnotationProcessor;
import org.jboss.as.webservices.webserviceref.WSRefDDProcessor;
import org.jboss.wsf.spi.deployment.DeploymentAspect;

/**
 * @author alessio.soldano@jboss.com
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class WSDeploymentActivator {

    static void activate(final DeploymentProcessorTarget processorTarget, final boolean appclient) {
        if (!isModularEnvironment()) {
            return;
        }
        processorTarget.addDeploymentProcessor(WSExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_WS_REF_DESCRIPTOR, new WSRefDDProcessor());
        processorTarget.addDeploymentProcessor(WSExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_WS_REF_ANNOTATION, new WSRefAnnotationProcessor());
        processorTarget.addDeploymentProcessor(WSExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_WS, new WSDependenciesProcessor(!appclient));
        if (!appclient) {
            processorTarget.addDeploymentProcessor(WSExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_WEBSERVICES_CONTEXT_INJECTION, new WebServicesContextJndiSetupProcessor());
            processorTarget.addDeploymentProcessor(WSExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_WEBSERVICES_LIBRARY_FILTER, new WSLibraryFilterProcessor());
            processorTarget.addDeploymentProcessor(WSExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_WEBSERVICES_XML, new WebservicesDescriptorDeploymentProcessor());
            processorTarget.addDeploymentProcessor(WSExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_JBOSS_WEBSERVICES_XML, new JBossWebservicesDescriptorDeploymentProcessor());
            processorTarget.addDeploymentProcessor(WSExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_WEBSERVICES_ANNOTATION, new WebServiceAnnotationProcessor());
            processorTarget.addDeploymentProcessor(WSExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_JAXWS_EJB_INTEGRATION, new WSIntegrationProcessorJAXWS_EJB());
            processorTarget.addDeploymentProcessor(WSExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_JAXWS_HANDLER_CHAIN_ANNOTATION, new WSHandlerChainAnnotationProcessor());
            processorTarget.addDeploymentProcessor(WSExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_WS_JMS_INTEGRATION, new WSIntegrationProcessorJAXWS_JMS());
            processorTarget.addDeploymentProcessor(WSExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_JAXWS_ENDPOINT_CREATE_COMPONENT_DESCRIPTIONS, new WSIntegrationProcessorJAXWS_POJO());
            processorTarget.addDeploymentProcessor(WSExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_JAXWS_HANDLER_CREATE_COMPONENT_DESCRIPTIONS, new WSIntegrationProcessorJAXWS_HANDLER());
            processorTarget.addDeploymentProcessor(WSExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_WS_SERVICES_DEPS, new WSServiceDependenciesProcessor());
            processorTarget.addDeploymentProcessor(WSExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_WS_VERIFICATION, new WSClassVerificationProcessor());
            processorTarget.addDeploymentProcessor(WSExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_WS_VERIFICATION, new GracefulShutdownIntegrationProcessor());
            processorTarget.addDeploymentProcessor(WSExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_WS_UNIVERSAL_META_DATA_MODEL, new WSModelDeploymentProcessor());
            addDeploymentProcessors(processorTarget, Phase.INSTALL, Phase.INSTALL_WS_DEPLOYMENT_ASPECTS);
        }
    }

    private static void addDeploymentProcessors(final DeploymentProcessorTarget processorTarget, final Phase phase, final int priority) {
        int index = 1;
        List<DeploymentAspect> aspects = DeploymentAspectsProvider.getSortedDeploymentAspects();
        for (final DeploymentAspect da : aspects) {
            if (WSLogger.ROOT_LOGGER.isTraceEnabled()) {
                WSLogger.ROOT_LOGGER.tracef("Installing aspect %s", da.getClass().getName());
            }
            processorTarget.addDeploymentProcessor(WSExtension.SUBSYSTEM_NAME, phase, priority + index++, new AspectDeploymentProcessor(da));
        }
    }

    private static boolean isModularEnvironment() {
        try {
            ModuleClassLoaderProvider.getDefaultProvider().getServerIntegrationClassLoader();
            return true;
        } catch (Exception e) {
            WSLogger.ROOT_LOGGER.couldNotActivateSubsystem(e);
            return false;
        }
    }
}
