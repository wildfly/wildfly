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

package org.jboss.as.jaxrs;


import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.jaxrs.deployment.JaxrsAnnotationProcessor;
import org.jboss.as.jaxrs.deployment.JaxrsCdiIntegrationProcessor;
import org.jboss.as.jaxrs.deployment.JaxrsComponentDeployer;
import org.jboss.as.jaxrs.deployment.JaxrsDependencyProcessor;
import org.jboss.as.jaxrs.deployment.JaxrsIntegrationProcessor;
import org.jboss.as.jaxrs.deployment.JaxrsMethodParameterProcessor;
import org.jboss.as.jaxrs.deployment.JaxrsScanningProcessor;
import org.jboss.as.jaxrs.deployment.JaxrsSpringProcessor;
import org.jboss.as.jaxrs.logging.JaxrsLogger;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.resteasy.spi.ResteasyDeployment;

/**
 * The jaxrs subsystem add update handler.
 *
 * @author Stuart Douglas
 * @author <a href="mailto:rsigal@redhat.com">Ron Sigal</a>
 */
class JaxrsSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final JaxrsSubsystemAdd INSTANCE = new JaxrsSubsystemAdd();

    JaxrsSubsystemAdd(AttributeDefinition... attributes) {
        super(attributes);
    }

    protected void performBoottime(final OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {

        final ServiceTarget serviceTarget = context.getServiceTarget();
        JaxrsLogger.JAXRS_LOGGER.resteasyVersion(ResteasyDeployment.class.getPackage().getImplementationVersion());
        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(JaxrsExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_JAXRS_ANNOTATIONS, new JaxrsAnnotationProcessor());
                processorTarget.addDeploymentProcessor(JaxrsExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_JAXRS_SPRING, new JaxrsSpringProcessor(serviceTarget));
                processorTarget.addDeploymentProcessor(JaxrsExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_JAXRS, new JaxrsDependencyProcessor());
                processorTarget.addDeploymentProcessor(JaxrsExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_JAXRS_SCANNING, new JaxrsScanningProcessor());
                processorTarget.addDeploymentProcessor(JaxrsExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_JAXRS_COMPONENT, new JaxrsComponentDeployer());

                CapabilityServiceSupport capabilities = context.getCapabilityServiceSupport();
                if (capabilities.hasCapability(WELD_CAPABILITY_NAME)) {
                    processorTarget.addDeploymentProcessor(JaxrsExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_JAXRS_CDI_INTEGRATION, new JaxrsCdiIntegrationProcessor());
                }
                processorTarget.addDeploymentProcessor(JaxrsExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_JAXRS_METHOD_PARAMETER, new JaxrsMethodParameterProcessor());

                processorTarget.addDeploymentProcessor(JaxrsExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_JAXRS_DEPLOYMENT, new JaxrsIntegrationProcessor());
            }
        }, OperationContext.Stage.RUNTIME);

        JaxrsServerConfig serverConfig = createServerConfig(operation, context);
        JaxrsServerConfigService.install(serviceTarget, serverConfig);
    }

    private static JaxrsServerConfig createServerConfig(ModelNode configuration, OperationContext context) throws OperationFailedException {
        final JaxrsServerConfig config = new JaxrsServerConfig();
        if (configuration.hasDefined(JaxrsConstants.JAXRS_2_0_REQUEST_MATCHING)) {
            config.setJaxrs20RequestMatching(JaxrsAttribute.JAXRS_2_0_REQUEST_MATCHING.resolveModelAttribute(context, configuration));
        }
        if (configuration.hasDefined(JaxrsConstants.RESTEASY_ADD_CHARSET)) {
            config.setResteasyAddCharset(JaxrsAttribute.RESTEASY_ADD_CHARSET.resolveModelAttribute(context, configuration));
        }
        if (configuration.hasDefined(JaxrsConstants.RESTEASY_BUFFER_EXCEPTION_ENTITY)) {
            config.setResteasyBufferExceptionEntity(JaxrsAttribute.RESTEASY_BUFFER_EXCEPTION_ENTITY.resolveModelAttribute(context, configuration));
        }
        if (configuration.hasDefined(JaxrsConstants.RESTEASY_DISABLE_HTML_SANITIZER)) {
            config.setResteasyDisableHtmlSanitizer(JaxrsAttribute.RESTEASY_DISABLE_HTML_SANITIZER.resolveModelAttribute(context, configuration));
        }
        if (configuration.hasDefined(JaxrsConstants.RESTEASY_DISABLE_PROVIDERS)) {
            config.setResteasyDisableProviders(JaxrsAttribute.RESTEASY_DISABLE_PROVIDERS.resolveModelAttribute(context, configuration));
        }
        if (configuration.hasDefined(JaxrsConstants.RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES)) {
            config.setResteasyDocumentExpandEntityReferences(JaxrsAttribute.RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES.resolveModelAttribute(context, configuration));
        }
        if (configuration.hasDefined(JaxrsConstants.RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS)) {
            config.setResteasyDocumentExpandEntityReferences(JaxrsAttribute.RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS.resolveModelAttribute(context, configuration));
        }
        if (configuration.hasDefined(JaxrsConstants.RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE)) {
            config.setResteasyDocumentSecureProcessingFeature(JaxrsAttribute.RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE.resolveModelAttribute(context, configuration));
        }
        if (configuration.hasDefined(JaxrsConstants.RESTEASY_GZIP_MAX_INPUT)) {
            config.setResteasyGzipMaxInput(JaxrsAttribute.RESTEASY_GZIP_MAX_INPUT.resolveModelAttribute(context, configuration));
        }
        if (configuration.hasDefined(JaxrsConstants.RESTEASY_JNDI_RESOURCES)) {
            config.setResteasyJndiResources(JaxrsAttribute.RESTEASY_JNDI_RESOURCES.resolveModelAttribute(context, configuration));
        }
        if (configuration.hasDefined(JaxrsConstants.RESTEASY_LANGUAGE_MAPPINGS)) {
            config.setResteasyLanguageMappings(JaxrsAttribute.RESTEASY_LANGUAGE_MAPPINGS.resolveModelAttribute(context, configuration));
        }
        if (configuration.hasDefined(JaxrsConstants.RESTEASY_MEDIA_TYPE_MAPPINGS)) {
            config.setResteasyMediaTypeMappings(JaxrsAttribute.RESTEASY_MEDIA_TYPE_MAPPINGS.resolveModelAttribute(context, configuration));
        }
        if (configuration.hasDefined(JaxrsConstants.RESTEASY_MEDIA_TYPE_PARAM_MAPPING)) {
            config.setResteasyMediaTypeParamMapping(JaxrsAttribute.RESTEASY_MEDIA_TYPE_PARAM_MAPPING.resolveModelAttribute(context, configuration));
        }
        if (configuration.hasDefined(JaxrsConstants.RESTEASY_ORIGINAL_WEBAPPLICATIONEXCEPTION_BEHAVIOR)) {
            config.setResteasyOriginalWebApplicationExceptionBehavior(JaxrsAttribute.RESTEASY_ORIGINAL_WEBAPPLICATIONEXCEPTION_BEHAVIOR.resolveModelAttribute(context, configuration));
        }
        if (configuration.hasDefined(JaxrsConstants.RESTEASY_PREFER_JACKSON_OVER_JSONB)) {
            config.setResteasyPreferJacksonOverJsonB(JaxrsAttribute.RESTEASY_PREFER_JACKSON_OVER_JSONB.resolveModelAttribute(context, configuration));
        }
        if (configuration.hasDefined(JaxrsConstants.RESTEASY_PROVIDERS)) {
            config.setResteasyProviders(JaxrsAttribute.RESTEASY_PROVIDERS.resolveModelAttribute(context, configuration));
        }
        if (configuration.hasDefined(JaxrsConstants.RESTEASY_RFC7232_PRECONDITIONS)) {
            config.setResteasyRFC7232Preconditions(JaxrsAttribute.RESTEASY_RFC7232_PRECONDITIONS.resolveModelAttribute(context, configuration));
        }
        if (configuration.hasDefined(JaxrsConstants.RESTEASY_ROLE_BASED_SECURITY)) {
            config.setResteasyRoleBasedSecurity(JaxrsAttribute.RESTEASY_ROLE_BASED_SECURITY.resolveModelAttribute(context, configuration));
        }
        if (configuration.hasDefined(JaxrsConstants.RESTEASY_SECURE_RANDOM_MAX_USE)) {
            config.setResteasySecureDisableDTDs(JaxrsAttribute.RESTEASY_SECURE_RANDOM_MAX_USE.resolveModelAttribute(context, configuration));
        }
        if (configuration.hasDefined(JaxrsConstants.RESTEASY_USE_BUILTIN_PROVIDERS)) {
            config.setResteasyUseBuiltinProviders(JaxrsAttribute.RESTEASY_USE_BUILTIN_PROVIDERS.resolveModelAttribute(context, configuration));
        }
        if (configuration.hasDefined(JaxrsConstants.RESTEASY_USE_CONTAINER_FORM_PARAMS)) {
            config.setResteasyUseContainerFormParams(JaxrsAttribute.RESTEASY_USE_CONTAINER_FORM_PARAMS.resolveModelAttribute(context, configuration));
        }
        if (configuration.hasDefined(JaxrsConstants.RESTEASY_WIDER_REQUEST_MATCHING)) {
            config.setResteasyWiderRequestMatching(JaxrsAttribute.RESTEASY_WIDER_REQUEST_MATCHING.resolveModelAttribute(context, configuration));
        }
        return config;
    }
}
