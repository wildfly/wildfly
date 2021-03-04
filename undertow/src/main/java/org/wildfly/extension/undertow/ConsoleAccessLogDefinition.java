/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.undertow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import io.undertow.predicate.Predicate;
import io.undertow.predicate.Predicates;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.DynamicNameMappers;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.xnio.XnioWorker;

/**
 * The resource definition for the {@code setting=console-access-log} resource.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class ConsoleAccessLogDefinition extends PersistentResourceDefinition {
    private static final RuntimeCapability<Void> CONSOLE_ACCESS_LOG_CAPABILITY = RuntimeCapability.Builder.of(
            Capabilities.CAPABILITY_CONSOLE_ACCESS_LOG, true, EventLoggerService.class)
            .setDynamicNameMapper(DynamicNameMappers.GRAND_PARENT)
            .build();

    static final SimpleAttributeDefinition INCLUDE_HOST_NAME = SimpleAttributeDefinitionBuilder.create("include-host-name", ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(ModelNode.TRUE)
            .setRestartAllServices()
            .build();

    static final PropertiesAttributeDefinition METADATA = new PropertiesAttributeDefinition.Builder("metadata", true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    static final Collection<AttributeDefinition> ATTRIBUTES = Arrays.asList(
            ExchangeAttributeDefinitions.ATTRIBUTES,
            INCLUDE_HOST_NAME,
            AccessLogDefinition.WORKER,
            AccessLogDefinition.PREDICATE,
            METADATA
    );
    static final ConsoleAccessLogDefinition INSTANCE = new ConsoleAccessLogDefinition();


    private ConsoleAccessLogDefinition() {
        super(new SimpleResourceDefinition.Parameters(PathElement.pathElement(Constants.SETTING, Constants.CONSOLE_ACCESS_LOG),
                UndertowExtension.getResolver(Constants.CONSOLE_ACCESS_LOG))
                .setAddHandler(AddHandler.INSTANCE)
                .setRemoveHandler(RemoveHandler.INSTANCE)
                .addCapabilities(CONSOLE_ACCESS_LOG_CAPABILITY)
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    private static class AddHandler extends AbstractAddStepHandler {
        static final AddHandler INSTANCE = new AddHandler();

        private AddHandler() {
            super(ATTRIBUTES);
        }

        @Override
        protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
            final PathAddress address = context.getCurrentAddress();
            final PathAddress hostAddress = address.getParent();
            final PathAddress serverAddress = hostAddress.getParent();

            final String worker = AccessLogDefinition.WORKER.resolveModelAttribute(context, model).asString();
            final ModelNode properties = METADATA.resolveModelAttribute(context, model);
            final Map<String, Object> metadata = new LinkedHashMap<>();
            if (properties.isDefined()) {
                for (Property property : properties.asPropertyList()) {
                    metadata.put(property.getName(), property.getValue().asString());
                }
            }

            Predicate predicate = null;
            final ModelNode predicateNode = AccessLogDefinition.PREDICATE.resolveModelAttribute(context, model);
            if (predicateNode.isDefined()) {
                predicate = Predicates.parse(predicateNode.asString(), getClass().getClassLoader());
            }

            final boolean includeHostName = INCLUDE_HOST_NAME.resolveModelAttribute(context, model).asBoolean();

            final String serverName = serverAddress.getLastElement().getValue();
            final String hostName = hostAddress.getLastElement().getValue();

            final ServiceBuilder<?> serviceBuilder = context.getServiceTarget()
                    .addService(CONSOLE_ACCESS_LOG_CAPABILITY.getCapabilityServiceName(address));

            final Supplier<Host> hostSupplier = serviceBuilder.requires(
                    context.getCapabilityServiceName(Capabilities.CAPABILITY_HOST, Host.class, serverName, hostName));
            final Supplier<XnioWorker> workerSupplier = serviceBuilder.requires(
                    context.getCapabilityServiceName(Capabilities.REF_IO_WORKER, XnioWorker.class, worker));

            // Get the list of attributes to log
            final Collection<AccessLogAttribute> attributes = parseAttributes(context, model);

            final EventLoggerService service = new EventLoggerService(attributes, predicate, metadata, includeHostName, hostSupplier,
                    workerSupplier);
            serviceBuilder.setInstance(service)
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();
        }

        private Collection<AccessLogAttribute> parseAttributes(final OperationContext context, final ModelNode model) throws OperationFailedException {
            final Collection<AccessLogAttribute> attributes = new ArrayList<>();
            final ModelNode attributesModel = ExchangeAttributeDefinitions.ATTRIBUTES.resolveModelAttribute(context, model);
            for (AttributeDefinition valueType : ExchangeAttributeDefinitions.ATTRIBUTES.getValueTypes()) {
                attributes.addAll(ExchangeAttributeDefinitions.resolveAccessLogAttribute(valueType, context, attributesModel));
            }
            return attributes;
        }
    }

    private static class RemoveHandler extends AbstractRemoveStepHandler {

        static final RemoveHandler INSTANCE = new RemoveHandler();

        @Override
        protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
            final PathAddress address = context.getCurrentAddress();
            context.removeService(CONSOLE_ACCESS_LOG_CAPABILITY.getCapabilityServiceName(address));
        }

        @Override
        protected void recoverServices(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
            AddHandler.INSTANCE.performRuntime(context, operation, model);
        }
    }
}
