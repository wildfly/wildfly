/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.appclient.subsystem;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.appclient.deployment.ActiveApplicationClientProcessor;
import org.jboss.as.appclient.deployment.AppClientJBossAllParser;
import org.jboss.as.appclient.deployment.ApplicationClientDependencyProcessor;
import org.jboss.as.appclient.logging.AppClientLogger;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.appclient.deployment.ApplicationClientManifestProcessor;
import org.jboss.as.appclient.deployment.ApplicationClientParsingDeploymentProcessor;
import org.jboss.as.appclient.deployment.ApplicationClientStartProcessor;
import org.jboss.as.appclient.service.ApplicationClientDeploymentService;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.management.Capabilities;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.jbossallxml.JBossAllXmlParserRegisteringProcessor;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import static org.jboss.as.appclient.subsystem.AppClientSubsystemResourceDefinition.APPCLIENT_CAPABILITY;
import static org.jboss.as.appclient.subsystem.Constants.CONNECTION_PROPERTIES_URL;
import static org.jboss.as.appclient.subsystem.Constants.HOST_URL;

/**
 * Add operation handler for the application client subsystem.
 *
 * @author Stuart Douglas
 */
class AppClientSubsystemAdd extends AbstractBoottimeAddStepHandler {

    public static final ServiceName APP_CLIENT_URI_SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "ejbClientContext", "appClientUri");
    public static final ServiceName APP_CLIENT_EJB_PROPERTIES_SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "ejbClientContext", "appClientEjbProperties");

    static final AppClientSubsystemAdd INSTANCE = new AppClientSubsystemAdd();

    private final String[] EMPTY_STRING = new String[0];

    private AppClientSubsystemAdd() {
        //
    }

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for(AttributeDefinition attr : AppClientSubsystemResourceDefinition.ATTRIBUTES) {
            attr.validateAndSet(operation, model);
        }
    }

    protected void performBoottime(final OperationContext context, ModelNode operation, final ModelNode model) throws OperationFailedException {
        final String deployment = AppClientSubsystemResourceDefinition.DEPLOYMENT.resolveModelAttribute(context, model).asStringOrNull();
        final File file = new File(AppClientSubsystemResourceDefinition.FILE.resolveModelAttribute(context, model).asString());
        if (!file.exists()) {
            context.setRollbackOnly();
            throw AppClientLogger.ROOT_LOGGER.cannotFindAppClientFile(file.getAbsoluteFile());
        }
        final String hostUrl = model.hasDefined(HOST_URL) ? AppClientSubsystemResourceDefinition.HOST_URL.resolveModelAttribute(context, model).asString() : null;
        final String connectionPropertiesUrl = model.hasDefined(CONNECTION_PROPERTIES_URL) ? AppClientSubsystemResourceDefinition.CONNECTION_PROPERTIES_URL.resolveModelAttribute(context, model).asString() : null;
        final List<String> parameters = AppClientSubsystemResourceDefinition.PARAMETERS.unwrap(context,model);

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(Constants.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_REGISTER_JBOSS_ALL_APPCLIENT, new JBossAllXmlParserRegisteringProcessor<>(AppClientJBossAllParser.ROOT_ELEMENT, AppClientJBossAllParser.ATTACHMENT_KEY, new AppClientJBossAllParser()));
                processorTarget.addDeploymentProcessor(Constants.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_APP_CLIENT_XML, new ApplicationClientParsingDeploymentProcessor());
                processorTarget.addDeploymentProcessor(Constants.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_APPLICATION_CLIENT_MANIFEST, new ApplicationClientManifestProcessor());
                processorTarget.addDeploymentProcessor(Constants.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_APPLICATION_CLIENT_ACTIVE, new ActiveApplicationClientProcessor(deployment));
                processorTarget.addDeploymentProcessor(Constants.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_APPLICATION_CLIENT, new ApplicationClientDependencyProcessor());
                processorTarget.addDeploymentProcessor(Constants.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_APPLICATION_CLIENT, new ApplicationClientStartProcessor(parameters.toArray(EMPTY_STRING)));

            }
        }, OperationContext.Stage.RUNTIME);

        CapabilityServiceBuilder<?> builder = context.getCapabilityServiceTarget().addCapability(APPCLIENT_CAPABILITY);
        Consumer<ApplicationClientDeploymentService> consumer = builder.provides(APPCLIENT_CAPABILITY);
        Supplier<ModelControllerClientFactory> mcfSupplier = builder.requires(ModelControllerClientFactory.SERVICE_DESCRIPTOR);
        Supplier<Executor> executorSupplier = builder.requires(Capabilities.MANAGEMENT_EXECUTOR);
        builder.setInstance(new ApplicationClientDeploymentService(consumer, file, mcfSupplier, executorSupplier));
        builder.install();

        try {
            if(connectionPropertiesUrl != null) {
                context.getServiceTarget().addService(APP_CLIENT_URI_SERVICE_NAME, new ConstantService<>(null))
                        .install();
                context.getServiceTarget().addService(APP_CLIENT_EJB_PROPERTIES_SERVICE_NAME, new ConstantService<>(connectionPropertiesUrl))
                        .install();
            } else {
                URI uri;
                if (hostUrl == null) {
                    uri = new URI("remote+http://localhost:8080");
                } else {
                    uri = new URI(hostUrl);
                }
                context.getServiceTarget().addService(APP_CLIENT_URI_SERVICE_NAME, new ConstantService<>(uri))
                        .install();
                context.getServiceTarget().addService(APP_CLIENT_EJB_PROPERTIES_SERVICE_NAME, new ConstantService<>(connectionPropertiesUrl))
                        .install();
            }
        } catch (URISyntaxException e) {
            throw new OperationFailedException(e);
        }

    }

    private static final class ConstantService<T> implements Service<T> {

        private final T value;

        private ConstantService(T value) {
            this.value = value;
        }

        @Override
        public void start(StartContext startContext) throws StartException {

        }

        @Override
        public void stop(StopContext stopContext) {

        }

        @Override
        public T getValue() throws IllegalStateException, IllegalArgumentException {
            return value;
        }
    }
}
