/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import io.undertow.server.RequestStatistics;
import io.undertow.server.handlers.ActiveRequestTrackerHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.version.Stability;
import org.jboss.as.web.host.WebHost;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.wildfly.extension.undertow.filters.FilterRefDefinition;

import java.util.Collection;
import java.util.List;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class HostDefinition extends SimpleResourceDefinition {
    static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.HOST);
    public static final String DEFAULT_WEB_MODULE_DEFAULT = "ROOT.war";

    static final RuntimeCapability<Void> HOST_CAPABILITY = RuntimeCapability.Builder.of(Host.SERVICE_DESCRIPTOR)
            .addRequirements(Capabilities.CAPABILITY_UNDERTOW)
            .build();
    static final RuntimeCapability<Void> ACTIVE_REQUEST_TRACKING_CAPABILITY =
            RuntimeCapability.Builder.of(Capabilities.CAPABILITY_ACTIVE_REQUEST_TRACKING, true,
                            ActiveRequestTrackerService.class)
                    .setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_CHILD)
                    .build();

    static final StringListAttributeDefinition ALIAS = new StringListAttributeDefinition.Builder(Constants.ALIAS)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setElementValidator(new StringLengthValidator(1))
            .setAllowExpression(true)
            .setAttributeParser(AttributeParser.COMMA_DELIMITED_STRING_LIST)
            .setAttributeMarshaller(AttributeMarshaller.COMMA_STRING_LIST)
            .build();
    static final SimpleAttributeDefinition DEFAULT_WEB_MODULE = new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_WEB_MODULE, ModelType.STRING, true)
            .setRestartAllServices()
            .setValidator(new StringLengthValidator(1, true, false))
            .setDefaultValue(new ModelNode(DEFAULT_WEB_MODULE_DEFAULT))
            .build();

    static final SimpleAttributeDefinition DEFAULT_RESPONSE_CODE = new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_RESPONSE_CODE, ModelType.INT, true)
            .setRestartAllServices()
            .setValidator(new IntRangeValidator(400, 599, true, true))
            .setDefaultValue(new ModelNode(404))
            .setAllowExpression(true)
            .build();
    static final SimpleAttributeDefinition DISABLE_CONSOLE_REDIRECT = new SimpleAttributeDefinitionBuilder("disable-console-redirect", ModelType.BOOLEAN, true)
            .setRestartAllServices()
            .setDefaultValue(ModelNode.FALSE)
            .setAllowExpression(true)
            .build();
    static final SimpleAttributeDefinition QUEUE_REQUESTS_ON_START = new SimpleAttributeDefinitionBuilder("queue-requests-on-start", ModelType.BOOLEAN, true)
            .setRestartAllServices()
            .setDefaultValue(ModelNode.TRUE)
            .setAllowExpression(true)
            .build();

    static final SimpleAttributeDefinition ACTIVE_REQUEST_TRACKING_ENABLED =
            new SimpleAttributeDefinitionBuilder(Constants.ACTIVE_REQUEST_TRACKING + "-" + Constants.ENABLED, ModelType.BOOLEAN, true)
                    .setRestartAllServices()
                    .setDefaultValue(ModelNode.FALSE)
                    .setAllowExpression(true)
                    .setStability(Stability.PREVIEW)
                    .build();

    static final SimpleAttributeDefinition ACTIVE_REQUEST_TRACKING_PREDICATE =
            new SimpleAttributeDefinitionBuilder(Constants.ACTIVE_REQUEST_TRACKING + "-" + Constants.PREDICATE, ModelType.STRING, true)
            .setAllowExpression(true)
            .setValidator(PredicateValidator.INSTANCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setStability(Stability.PREVIEW)
            .build();

    static final Collection<AttributeDefinition> ATTRIBUTES = List.of(ALIAS, DEFAULT_WEB_MODULE, DEFAULT_RESPONSE_CODE,
            DISABLE_CONSOLE_REDIRECT, QUEUE_REQUESTS_ON_START, ACTIVE_REQUEST_TRACKING_ENABLED, ACTIVE_REQUEST_TRACKING_PREDICATE);

    private static final SimpleOperationDefinition LIST_ACTIVE_REQUESTS =
            new SimpleOperationDefinitionBuilder(Constants.LIST_ACTIVE_REQUESTS,
                    UndertowExtension.getResolver("active-request-tracker"))
                    .setReplyType(ModelType.LIST).setReplyValueType(ModelType.STRING)
                    .setRuntimeOnly()
                    .setStability(Stability.PREVIEW)
                    .build();

    HostDefinition() {
        super(new SimpleResourceDefinition.Parameters(PATH_ELEMENT, UndertowExtension.getResolver(PATH_ELEMENT.getKey()))
                .setAddHandler(HostAdd.INSTANCE)
                .setRemoveHandler(new HostRemove())
                .addCapabilities(HOST_CAPABILITY, WebHost.CAPABILITY)
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        for (AttributeDefinition attribute : ATTRIBUTES) {
            registration.registerReadWriteAttribute(attribute, null, ReloadRequiredWriteAttributeHandler.INSTANCE);
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registration) {
        registration.registerSubModel(new LocationDefinition());
        registration.registerSubModel(new AccessLogDefinition());
        registration.registerSubModel(new ConsoleAccessLogDefinition());
        registration.registerSubModel(new FilterRefDefinition());
        registration.registerSubModel(new HttpInvokerDefinition());
        new HostSingleSignOnDefinition().register(registration, null);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(LIST_ACTIVE_REQUESTS, new ListActiveRequestsOperationHandler());
    }

    private static class ListActiveRequestsOperationHandler implements OperationStepHandler {
        @Override
        public void execute(final OperationContext context, final ModelNode operation) {
            PathAddress hostPath = context.getCurrentAddress();
            PathAddress serverPath = hostPath.getParent();

            ServiceName serviceName = UndertowService.activeRequestTrackingServiceName(serverPath.getLastElement().getValue(),
                    hostPath.getLastElement().getValue());

            ServiceRegistry serviceRegistry = context.getServiceRegistry(false);
            ServiceController<?> controller = serviceRegistry.getService(serviceName);

            //check if deployment is active at all
            if (controller == null || controller.getState() != ServiceController.State.UP) {
                return;
            }

            ActiveRequestTrackerHandler handler = ((ActiveRequestTrackerService) controller.getService()).getValue();

            if (handler != null) {
                List<RequestStatistics> stats = handler.getTrackedRequests();

                if (!stats.isEmpty()) {
                    ModelNode result = new ModelNode();
                    stats.forEach(c -> {
                        ModelNode chanInfo = new ModelNode();
                        chanInfo.get("remote-address").set(c.getRemoteAddress());
                        chanInfo.get("uri").set(c.getUri());
                        chanInfo.get("http-method").set(c.getMethod());
                        chanInfo.get("protocol").set(c.getProtocol());
                        chanInfo.get("query-string").set(c.getQueryString());
                        chanInfo.get("bytes-received").set(c.getBytesReceived());
                        chanInfo.get("bytes-sent").set(c.getBytesSent());
                        chanInfo.get("start-time").set(c.getStartTime());
                        chanInfo.get("processing-time").set(c.getProcessingTime());
                        result.add(chanInfo);
                    });
                    context.getResult().set(result);
                }
            }
        }
    }

}
