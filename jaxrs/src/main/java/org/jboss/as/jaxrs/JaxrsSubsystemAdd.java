/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jaxrs;


import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import java.util.Iterator;
import java.util.function.Function;

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
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.spi.ResteasyDeployment;

/**
 * The jaxrs subsystem add update handler.
 *
 * @author Stuart Douglas
 * @author <a href="mailto:rsigal@redhat.com">Ron Sigal</a>
 */
class JaxrsSubsystemAdd extends AbstractBoottimeAddStepHandler {

    private static final Function<ModelNode, String> LIST_TO_STRING = (model) -> {
        final StringBuilder result = new StringBuilder();
        final Iterator<ModelNode> iterator = model.asList().iterator();
        while (iterator.hasNext()) {
            result.append(iterator.next().asString());
            if (iterator.hasNext()) {
                result.append(',');
            }
        }
        return result.toString();
    };

    private static final Function<ModelNode, String> MAP_TO_STRING = (model) -> {
        final StringBuilder result = new StringBuilder();
        final Iterator<Property> iterator = model.asPropertyList().iterator();
        while (iterator.hasNext()) {
            final Property property = iterator.next();
            result.append(property.getName()).append(':').append(property.getValue().asString());
            if (iterator.hasNext()) {
                result.append(',');
            }
        }
        return result.toString();
    };

    protected void performBoottime(final OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {

        final ServiceTarget serviceTarget = context.getServiceTarget();
        JaxrsLogger.JAXRS_LOGGER.resteasyVersion(ResteasyDeployment.class.getPackage().getImplementationVersion());

        final JaxrsServerConfig contextConfiguration = createServerConfig(operation, context);

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

                processorTarget.addDeploymentProcessor(JaxrsExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_JAXRS_DEPLOYMENT, new JaxrsIntegrationProcessor(contextConfiguration));
            }
        }, OperationContext.Stage.RUNTIME);
    }

    @SuppressWarnings("deprecation")
    private static JaxrsServerConfig createServerConfig(ModelNode configuration, OperationContext context) throws OperationFailedException {
        final JaxrsServerConfig config = new JaxrsServerConfig();
        addContextParameter(config, JaxrsAttribute.JAXRS_2_0_REQUEST_MATCHING, context, configuration);

        addContextParameter(config, JaxrsAttribute.RESTEASY_ADD_CHARSET, context, configuration);

        addContextParameter(config, JaxrsAttribute.RESTEASY_BUFFER_EXCEPTION_ENTITY, context, configuration);

        addContextParameter(config, JaxrsAttribute.RESTEASY_DISABLE_HTML_SANITIZER, context, configuration);

        addContextParameter(config, JaxrsAttribute.RESTEASY_DISABLE_PROVIDERS, context, configuration, LIST_TO_STRING);

        addContextParameter(config, JaxrsAttribute.RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES, context, configuration);

        addContextParameter(config, JaxrsAttribute.RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS, context, configuration);

        addContextParameter(config, JaxrsAttribute.RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE, context, configuration);

        addContextParameter(config, JaxrsAttribute.RESTEASY_GZIP_MAX_INPUT, context, configuration);

        addContextParameter(config, JaxrsAttribute.RESTEASY_JNDI_RESOURCES, context, configuration, LIST_TO_STRING);

        addContextParameter(config, JaxrsAttribute.RESTEASY_LANGUAGE_MAPPINGS, context, configuration, MAP_TO_STRING);

        addContextParameter(config, JaxrsAttribute.RESTEASY_MEDIA_TYPE_MAPPINGS, context, configuration, MAP_TO_STRING);

        addContextParameter(config, JaxrsAttribute.RESTEASY_MEDIA_TYPE_PARAM_MAPPING, context, configuration);

        addContextParameter(config, JaxrsAttribute.RESTEASY_ORIGINAL_WEBAPPLICATIONEXCEPTION_BEHAVIOR, context, configuration);

        addContextParameter(config, JaxrsAttribute.RESTEASY_PATCHFILTER_DISABLED, context, configuration);

        addContextParameter(config, ResteasyContextParameters.RESTEASY_PREFER_JACKSON_OVER_JSONB,
                JaxrsAttribute.RESTEASY_PREFER_JACKSON_OVER_JSONB, context, configuration);

        addContextParameter(config, JaxrsAttribute.RESTEASY_PROVIDERS, context, configuration, LIST_TO_STRING);

        addContextParameter(config, JaxrsAttribute.RESTEASY_RFC7232_PRECONDITIONS, context, configuration);

        addContextParameter(config, JaxrsAttribute.RESTEASY_ROLE_BASED_SECURITY, context, configuration);

        addContextParameter(config, JaxrsAttribute.RESTEASY_SECURE_RANDOM_MAX_USE, context, configuration);

        addContextParameter(config, JaxrsAttribute.RESTEASY_USE_BUILTIN_PROVIDERS, context, configuration);

        addContextParameter(config, JaxrsAttribute.RESTEASY_USE_CONTAINER_FORM_PARAMS, context, configuration);

        addContextParameter(config, JaxrsAttribute.RESTEASY_WIDER_REQUEST_MATCHING, context, configuration);

        addContextParameter(config, "resteasy.server.tracing.threshold", JaxrsAttribute.TRACING_THRESHOLD, context, configuration);
        addContextParameter(config, "resteasy.server.tracing.type", JaxrsAttribute.TRACING_TYPE, context, configuration);
        return config;
    }

    private static void addContextParameter(final JaxrsServerConfig config, final AttributeDefinition attribute,
                                            final OperationContext context, final ModelNode model) throws OperationFailedException {
        addContextParameter(config, attribute, context, model, ModelNode::asString);
    }

    private static void addContextParameter(final JaxrsServerConfig config, final String contextName, final AttributeDefinition attribute,
                                            final OperationContext context, final ModelNode model) throws OperationFailedException {
        addContextParameter(config, contextName, attribute, context, model, ModelNode::asString);
    }

    private static void addContextParameter(final JaxrsServerConfig config, final AttributeDefinition attribute,
                                            final OperationContext context, final ModelNode model,
                                            final Function<ModelNode, String> valueMapper) throws OperationFailedException {
        if (model.hasDefined(attribute.getName())) {
            final String name = attribute.getXmlName().replace('-', '.');
            addContextParameter(config, name, valueMapper.apply(attribute.resolveModelAttribute(context, model)));
        }
    }

    private static void addContextParameter(final JaxrsServerConfig config, final String contextName,
                                            final AttributeDefinition attribute, final OperationContext context,
                                            final ModelNode model, final Function<ModelNode, String> valueMapper) throws OperationFailedException {
        if (model.hasDefined(attribute.getName())) {
            final String name = contextName == null ? attribute.getXmlName().replace('-', '.') : contextName;
            addContextParameter(config, name, valueMapper.apply(attribute.resolveModelAttribute(context, model)));
        }
    }

    private static void addContextParameter(final JaxrsServerConfig config, final String name, final String value) {
        config.putContextParameter(name, value);
    }
}
