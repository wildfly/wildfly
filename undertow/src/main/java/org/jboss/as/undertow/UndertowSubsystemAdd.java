package org.jboss.as.undertow;

import java.util.List;

import io.undertow.Version;
import org.jboss.as.clustering.web.DistributedCacheManagerFactory;
import org.jboss.as.clustering.web.DistributedCacheManagerFactoryService;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.jbossallxml.JBossAllXmlParserRegisteringProcessor;
import org.jboss.as.undertow.deployment.ELExpressionFactoryProcessor;
import org.jboss.as.undertow.deployment.EarContextRootProcessor;
import org.jboss.as.undertow.deployment.JBossWebParsingDeploymentProcessor;
import org.jboss.as.undertow.deployment.ServletContainerInitializerDeploymentProcessor;
import org.jboss.as.undertow.deployment.TldParsingDeploymentProcessor;
import org.jboss.as.undertow.deployment.UndertowDependencyProcessor;
import org.jboss.as.undertow.deployment.UndertowDeploymentProcessor;
import org.jboss.as.undertow.deployment.UndertowJSRWebSocketDeploymentProcessor;
import org.jboss.as.undertow.deployment.WarAnnotationDeploymentProcessor;
import org.jboss.as.undertow.deployment.WarDeploymentInitializingProcessor;
import org.jboss.as.undertow.deployment.WarMetaDataProcessor;
import org.jboss.as.undertow.deployment.WarStructureDeploymentProcessor;
import org.jboss.as.undertow.deployment.WebFragmentParsingDeploymentProcessor;
import org.jboss.as.undertow.deployment.WebJBossAllParser;
import org.jboss.as.undertow.deployment.WebParsingDeploymentProcessor;
import org.jboss.as.undertow.session.JvmRouteRegistryEntryProviderService;
import org.jboss.as.web.common.SharedTldsMetaDataBuilder;
import org.jboss.as.web.host.CommonWebServer;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;


/**
 * Handler responsible for adding the subsystem resource to the model
 *
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
class UndertowSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final UndertowSubsystemAdd INSTANCE = new UndertowSubsystemAdd();

    private UndertowSubsystemAdd() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition def : UndertowRootDefinition.ATTRIBUTES) {
            def.validateAndSet(operation, model);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void performBoottime(OperationContext context, ModelNode operation, final ModelNode model,
                                ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {

        final String defaultVirtualHost = UndertowRootDefinition.DEFAULT_VIRTUAL_HOST.resolveModelAttribute(context, model).asString();
        final String defaultContainer = UndertowRootDefinition.DEFAULT_SERVLET_CONTAINER.resolveModelAttribute(context, model).asString();
        final String defaultServer = UndertowRootDefinition.DEFAULT_SERVER.resolveModelAttribute(context, model).asString();

        final ModelNode instanceIdModel = UndertowRootDefinition.INSTANCE_ID.resolveModelAttribute(context, model);
        final String instanceId = instanceIdModel.isDefined() ? instanceIdModel.asString() : null;

        newControllers.add(context.getServiceTarget().addService(UndertowService.UNDERTOW, new UndertowService(defaultContainer, defaultServer, defaultVirtualHost, instanceId))
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install());

        newControllers.add(context.getServiceTarget().addService(CommonWebServer.SERVICE_NAME, new WebServerService())
                .install());


        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            protected void execute(DeploymentProcessorTarget processorTarget) {

                final SharedWebMetaDataBuilder sharedWebBuilder = new SharedWebMetaDataBuilder(model.clone());
                final SharedTldsMetaDataBuilder sharedTldsBuilder = new SharedTldsMetaDataBuilder(model.clone());

                processorTarget.addDeploymentProcessor(UndertowExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_REGISTER_JBOSS_ALL_XML_PARSER, new JBossAllXmlParserRegisteringProcessor<>(WebJBossAllParser.ROOT_ELEMENT, WebJBossAllParser.ATTACHMENT_KEY, new WebJBossAllParser()));
                processorTarget.addDeploymentProcessor(UndertowExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_WAR_DEPLOYMENT_INIT, new WarDeploymentInitializingProcessor());
                processorTarget.addDeploymentProcessor(UndertowExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_WAR, new WarStructureDeploymentProcessor(sharedWebBuilder.create(), sharedTldsBuilder));
                processorTarget.addDeploymentProcessor(UndertowExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_WEB_DEPLOYMENT, new WebParsingDeploymentProcessor());
                processorTarget.addDeploymentProcessor(UndertowExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_WEB_DEPLOYMENT_FRAGMENT, new WebFragmentParsingDeploymentProcessor());
                processorTarget.addDeploymentProcessor(UndertowExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_JBOSS_WEB_DEPLOYMENT, new JBossWebParsingDeploymentProcessor());
                processorTarget.addDeploymentProcessor(UndertowExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_ANNOTATION_WAR, new WarAnnotationDeploymentProcessor());
                processorTarget.addDeploymentProcessor(UndertowExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_EAR_CONTEXT_ROOT, new EarContextRootProcessor());
                processorTarget.addDeploymentProcessor(UndertowExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_WEB_MERGE_METADATA, new WarMetaDataProcessor());
                processorTarget.addDeploymentProcessor(UndertowExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_WEB_MERGE_METADATA + 1, new TldParsingDeploymentProcessor()); //todo: fix priority
                processorTarget.addDeploymentProcessor(UndertowExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_WEB_MERGE_METADATA + 2, new org.jboss.as.undertow.deployment.WebComponentProcessor()); //todo: fix priority

                processorTarget.addDeploymentProcessor(UndertowExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_WAR_MODULE, new UndertowDependencyProcessor());

                processorTarget.addDeploymentProcessor(UndertowExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EL_EXPRESSION_FACTORY, new ELExpressionFactoryProcessor());
                processorTarget.addDeploymentProcessor(UndertowExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_UNDERTOW_WEBSOCKETS, new UndertowJSRWebSocketDeploymentProcessor());


                processorTarget.addDeploymentProcessor(UndertowExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_SERVLET_INIT_DEPLOYMENT, new ServletContainerInitializerDeploymentProcessor());

                processorTarget.addDeploymentProcessor(UndertowExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_WAR_DEPLOYMENT, new UndertowDeploymentProcessor(defaultVirtualHost, defaultContainer, defaultServer));

            }
        }, OperationContext.Stage.RUNTIME);

        UndertowLogger.ROOT_LOGGER.serverStarting(Version.getVersionString());

        final DistributedCacheManagerFactory factory = new DistributedCacheManagerFactoryService().getValue();
        if (factory != null) {
            newControllers.add(context.getServiceTarget().addService(DistributedCacheManagerFactoryService.JVM_ROUTE_REGISTRY_ENTRY_PROVIDER_SERVICE_NAME, new JvmRouteRegistryEntryProviderService(instanceId))
                    .setInitialMode(ServiceController.Mode.ON_DEMAND)
                    .install());
            newControllers.addAll(factory.installServices(context.getServiceTarget()));
        }
    }

}
