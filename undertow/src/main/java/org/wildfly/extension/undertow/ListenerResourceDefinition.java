/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.registry.AttributeAccess.Flag.COUNTER_METRIC;
import static org.wildfly.extension.undertow.Capabilities.REF_IO_WORKER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.undertow.UndertowOptions;
import io.undertow.server.ConnectorStatistics;
import io.undertow.server.handlers.ChannelUpgradeHandler;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.io.OptionAttributeDefinition;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;
import org.xnio.Options;

/**
 * @author Tomaz Cerar
 * @author Stuart Douglas
 */
abstract class ListenerResourceDefinition extends PersistentResourceDefinition {

    static final RuntimeCapability<Void> LISTENER_CAPABILITY = RuntimeCapability.Builder.of(Capabilities.CAPABILITY_LISTENER, true, UndertowListener.class)
            .setDynamicNameMapper(UnaryCapabilityNameResolver.DEFAULT)
            .setAllowMultipleRegistrations(true)
            .build();

    static final RuntimeCapability<Void> SERVER_LISTENER_CAPABILITY = RuntimeCapability.Builder.of(Capabilities.CAPABILITY_SERVER_LISTENER, true, UndertowListener.class)
            .setDynamicNameMapper(BinaryCapabilityNameResolver.PARENT_CHILD)
            .setAllowMultipleRegistrations(true)
            .build();

    // only used by the subclasses Http(s)ListenerResourceDefinition
    static final RuntimeCapability<Void> HTTP_UPGRADE_REGISTRY_CAPABILITY = RuntimeCapability.Builder.of(Capabilities.CAPABILITY_HTTP_UPGRADE_REGISTRY, true, ChannelUpgradeHandler.class)
            .setAllowMultipleRegistrations(true)
            .build();

    static final SimpleAttributeDefinition SOCKET_BINDING = new SimpleAttributeDefinitionBuilder(Constants.SOCKET_BINDING, ModelType.STRING)
            .setRequired(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1))
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .setCapabilityReference(CapabilityReferenceRecorder.builder(LISTENER_CAPABILITY, SocketBinding.SERVICE_DESCRIPTOR).build())
            .build();

    static final SimpleAttributeDefinition WORKER = new SimpleAttributeDefinitionBuilder(Constants.WORKER, ModelType.STRING)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1))
            .setDefaultValue(new ModelNode("default"))
            .setCapabilityReference(REF_IO_WORKER, LISTENER_CAPABILITY)
            .build();

    static final SimpleAttributeDefinition BUFFER_POOL = new SimpleAttributeDefinitionBuilder(Constants.BUFFER_POOL, ModelType.STRING)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1))
            .setDefaultValue(new ModelNode("default"))
            .setCapabilityReference(Capabilities.CAPABILITY_BYTE_BUFFER_POOL, LISTENER_CAPABILITY)
            .build();

    static final SimpleAttributeDefinition ENABLED = new SimpleAttributeDefinitionBuilder(Constants.ENABLED, ModelType.BOOLEAN)
            .setRequired(false)
            .setDeprecated(ModelVersion.create(3, 2))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(ModelNode.TRUE)
            .setAllowExpression(true)
            .build();

    static final SimpleAttributeDefinition REDIRECT_SOCKET = new SimpleAttributeDefinitionBuilder(Constants.REDIRECT_SOCKET, ModelType.STRING)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setAllowExpression(false)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .setCapabilityReference(CapabilityReferenceRecorder.builder(LISTENER_CAPABILITY, SocketBinding.SERVICE_DESCRIPTOR).build())
            .build();

    static final SimpleAttributeDefinition RESOLVE_PEER_ADDRESS = new SimpleAttributeDefinitionBuilder(Constants.RESOLVE_PEER_ADDRESS, ModelType.BOOLEAN)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setDefaultValue(ModelNode.FALSE)
            .setAllowExpression(true)
            .build();

    static final StringListAttributeDefinition DISALLOWED_METHODS = new StringListAttributeDefinition.Builder(Constants.DISALLOWED_METHODS)
            .setDefaultValue(new ModelNode().add("TRACE"))
            .setRequired(false)
            .setValidator(new StringLengthValidator(0))
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setAllowExpression(true)
            .build();

    static final SimpleAttributeDefinition SECURE = new SimpleAttributeDefinitionBuilder(Constants.SECURE, ModelType.BOOLEAN)
            .setDefaultValue(ModelNode.FALSE)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setAllowExpression(true)
            .build();

    static final OptionAttributeDefinition BACKLOG = OptionAttributeDefinition.builder("tcp-backlog", Options.BACKLOG).setDefaultValue(new ModelNode(10000)).setAllowExpression(true).setValidator(new IntRangeValidator(1)).build();
    static final OptionAttributeDefinition RECEIVE_BUFFER = OptionAttributeDefinition.builder("receive-buffer", Options.RECEIVE_BUFFER).setAllowExpression(true).setValidator(new IntRangeValidator(1)).build();
    static final OptionAttributeDefinition SEND_BUFFER = OptionAttributeDefinition.builder("send-buffer", Options.SEND_BUFFER).setAllowExpression(true).setValidator(new IntRangeValidator(1)).build();
    static final OptionAttributeDefinition KEEP_ALIVE = OptionAttributeDefinition.builder("tcp-keep-alive", Options.KEEP_ALIVE).setAllowExpression(true).build();
    static final OptionAttributeDefinition READ_TIMEOUT = OptionAttributeDefinition.builder("read-timeout", Options.READ_TIMEOUT).setDefaultValue(new ModelNode(90000)).setAllowExpression(true).setMeasurementUnit(MeasurementUnit.MILLISECONDS).build();
    static final OptionAttributeDefinition WRITE_TIMEOUT = OptionAttributeDefinition.builder("write-timeout", Options.WRITE_TIMEOUT).setDefaultValue(new ModelNode(90000)).setAllowExpression(true).setMeasurementUnit(MeasurementUnit.MILLISECONDS).build();
    static final OptionAttributeDefinition MAX_CONNECTIONS = OptionAttributeDefinition.builder(Constants.MAX_CONNECTIONS, Options.CONNECTION_HIGH_WATER).setValidator(new IntRangeValidator(1)).setAllowExpression(true).build();

    static final OptionAttributeDefinition MAX_HEADER_SIZE = OptionAttributeDefinition.builder("max-header-size", UndertowOptions.MAX_HEADER_SIZE).setDefaultValue(new ModelNode(UndertowOptions.DEFAULT_MAX_HEADER_SIZE)).setAllowExpression(true).setMeasurementUnit(MeasurementUnit.BYTES).setValidator(new IntRangeValidator(1)).build();
    static final OptionAttributeDefinition MAX_ENTITY_SIZE = OptionAttributeDefinition.builder(Constants.MAX_POST_SIZE, UndertowOptions.MAX_ENTITY_SIZE).setDefaultValue(new ModelNode(10485760L)).setValidator(new LongRangeValidator(0)).setMeasurementUnit(MeasurementUnit.BYTES).setAllowExpression(true).build();
    static final OptionAttributeDefinition BUFFER_PIPELINED_DATA = OptionAttributeDefinition.builder("buffer-pipelined-data", UndertowOptions.BUFFER_PIPELINED_DATA).setDefaultValue(ModelNode.FALSE).setAllowExpression(true).build();
    static final OptionAttributeDefinition MAX_PARAMETERS = OptionAttributeDefinition.builder("max-parameters", UndertowOptions.MAX_PARAMETERS).setDefaultValue(new ModelNode(1000)).setValidator(new IntRangeValidator(1)).setAllowExpression(true).build();
    static final OptionAttributeDefinition MAX_HEADERS = OptionAttributeDefinition.builder("max-headers", UndertowOptions.MAX_HEADERS).setDefaultValue(new ModelNode(200)).setValidator(new IntRangeValidator(1)).setAllowExpression(true).build();
    static final OptionAttributeDefinition MAX_COOKIES = OptionAttributeDefinition.builder("max-cookies", UndertowOptions.MAX_COOKIES).setDefaultValue(new ModelNode(200)).setValidator(new IntRangeValidator(1)).setAllowExpression(true).build();
    static final OptionAttributeDefinition ALLOW_ENCODED_SLASH = OptionAttributeDefinition.builder("allow-encoded-slash", UndertowOptions.ALLOW_ENCODED_SLASH).setDefaultValue(ModelNode.FALSE).setAllowExpression(true).build();
    static final OptionAttributeDefinition DECODE_URL = OptionAttributeDefinition.builder("decode-url", UndertowOptions.DECODE_URL).setDefaultValue(ModelNode.TRUE).setAllowExpression(true).build();
    static final OptionAttributeDefinition URL_CHARSET = OptionAttributeDefinition.builder("url-charset", UndertowOptions.URL_CHARSET).setDefaultValue(new ModelNode("UTF-8")).setAllowExpression(true).build();
    static final OptionAttributeDefinition ALWAYS_SET_KEEP_ALIVE = OptionAttributeDefinition.builder("always-set-keep-alive", UndertowOptions.ALWAYS_SET_KEEP_ALIVE).setDefaultValue(ModelNode.TRUE).setAllowExpression(true).build();
    static final OptionAttributeDefinition MAX_BUFFERED_REQUEST_SIZE = OptionAttributeDefinition.builder(Constants.MAX_BUFFERED_REQUEST_SIZE, UndertowOptions.MAX_BUFFERED_REQUEST_SIZE).setDefaultValue(new ModelNode(16384)).setValidator(new IntRangeValidator(1)).setMeasurementUnit(MeasurementUnit.BYTES).setAllowExpression(true).build();
    static final OptionAttributeDefinition RECORD_REQUEST_START_TIME = OptionAttributeDefinition.builder("record-request-start-time", UndertowOptions.RECORD_REQUEST_START_TIME).setDefaultValue(ModelNode.FALSE).setAllowExpression(true).build();
    static final OptionAttributeDefinition ALLOW_EQUALS_IN_COOKIE_VALUE = OptionAttributeDefinition.builder("allow-equals-in-cookie-value", UndertowOptions.ALLOW_EQUALS_IN_COOKIE_VALUE).setDefaultValue(ModelNode.FALSE).setAllowExpression(true).build();
    static final OptionAttributeDefinition NO_REQUEST_TIMEOUT = OptionAttributeDefinition.builder("no-request-timeout", UndertowOptions.NO_REQUEST_TIMEOUT).setDefaultValue(new ModelNode(60000)).setMeasurementUnit(MeasurementUnit.MILLISECONDS).setRequired(false).setAllowExpression(true).build();
    static final OptionAttributeDefinition REQUEST_PARSE_TIMEOUT = OptionAttributeDefinition.builder("request-parse-timeout", UndertowOptions.REQUEST_PARSE_TIMEOUT).setMeasurementUnit(MeasurementUnit.MILLISECONDS).setRequired(false).setAllowExpression(true).build();
    static final OptionAttributeDefinition RFC6265_COOKIE_VALIDATION = OptionAttributeDefinition.builder("rfc6265-cookie-validation", UndertowOptions.ENABLE_RFC6265_COOKIE_VALIDATION).setDefaultValue(ModelNode.FALSE).setRequired(false).setAllowExpression(true).build();
    static final OptionAttributeDefinition ALLOW_UNESCAPED_CHARACTERS_IN_URL = OptionAttributeDefinition.builder("allow-unescaped-characters-in-url", UndertowOptions.ALLOW_UNESCAPED_CHARACTERS_IN_URL).setDefaultValue(ModelNode.FALSE).setRequired(false).setAllowExpression(true).build();
    static final OptionAttributeDefinition WEB_SOCKET_READ_TIMEOUT = OptionAttributeDefinition.builder(Constants.WEB_SOCKET_READ_TIMEOUT, Options.READ_TIMEOUT).setAllowExpression(true).setMeasurementUnit(MeasurementUnit.MILLISECONDS).setValidator(new LongRangeValidator(0, Long.MAX_VALUE, true, true)).setRequired(false).setStability(Stability.PREVIEW).build();
    static final OptionAttributeDefinition WEB_SOCKET_WRITE_TIMEOUT = OptionAttributeDefinition.builder(Constants.WEB_SOCKET_WRITE_TIMEOUT, Options.WRITE_TIMEOUT).setAllowExpression(true).setMeasurementUnit(MeasurementUnit.MILLISECONDS).setValidator(new LongRangeValidator(0, Long.MAX_VALUE, true, true)).setRequired(false).setStability(Stability.PREVIEW).build();

    public enum ConnectorStat {
        REQUEST_COUNT(new SimpleAttributeDefinitionBuilder("request-count", ModelType.LONG)
                .setUndefinedMetricValue(ModelNode.ZERO)
                .setFlags(COUNTER_METRIC)
                .setStorageRuntime()
                .build()),
        BYTES_SENT(new SimpleAttributeDefinitionBuilder("bytes-sent", ModelType.LONG)
                .setUndefinedMetricValue(ModelNode.ZERO)
                .setMeasurementUnit(MeasurementUnit.BYTES)
                .setFlags(COUNTER_METRIC)
                .setStorageRuntime()
                .build()),
        BYTES_RECEIVED(new SimpleAttributeDefinitionBuilder("bytes-received", ModelType.LONG)
                .setUndefinedMetricValue(ModelNode.ZERO)
                .setMeasurementUnit(MeasurementUnit.BYTES)
                .setFlags(COUNTER_METRIC)
                .setStorageRuntime()
                .build()),
        ERROR_COUNT(new SimpleAttributeDefinitionBuilder("error-count", ModelType.LONG)
                .setUndefinedMetricValue(ModelNode.ZERO)
                .setFlags(COUNTER_METRIC)
                .setStorageRuntime().build()),
        PROCESSING_TIME(new SimpleAttributeDefinitionBuilder("processing-time", ModelType.LONG)
                .setUndefinedMetricValue(ModelNode.ZERO)
                .setMeasurementUnit(MeasurementUnit.NANOSECONDS)
                .setFlags(COUNTER_METRIC)
                .setStorageRuntime()
                .build()),
        MAX_PROCESSING_TIME(new SimpleAttributeDefinitionBuilder("max-processing-time", ModelType.LONG)
                .setMeasurementUnit(MeasurementUnit.NANOSECONDS)
                .setUndefinedMetricValue(ModelNode.ZERO).setStorageRuntime().build());

        private static final Map<String, ConnectorStat> MAP = new HashMap<>();

        static {
            for (ConnectorStat stat : EnumSet.allOf(ConnectorStat.class)) {
                MAP.put(stat.toString(), stat);
            }
        }

        final AttributeDefinition definition;

        private ConnectorStat(final AttributeDefinition definition) {
            this.definition = definition;
        }

        @Override
        public final String toString() {
            return definition.getName();
        }

        public static synchronized ConnectorStat getStat(final String stringForm) {
            return MAP.get(stringForm);
        }
    }

    static final Collection<OptionAttributeDefinition> LISTENER_OPTIONS = List.of(MAX_HEADER_SIZE, MAX_ENTITY_SIZE,
            BUFFER_PIPELINED_DATA, MAX_PARAMETERS, MAX_HEADERS, MAX_COOKIES, ALLOW_ENCODED_SLASH, DECODE_URL,
            URL_CHARSET, ALWAYS_SET_KEEP_ALIVE, MAX_BUFFERED_REQUEST_SIZE, RECORD_REQUEST_START_TIME,
            ALLOW_EQUALS_IN_COOKIE_VALUE, NO_REQUEST_TIMEOUT, REQUEST_PARSE_TIMEOUT, RFC6265_COOKIE_VALIDATION,
            ALLOW_UNESCAPED_CHARACTERS_IN_URL);

    static final Collection<OptionAttributeDefinition> SOCKET_OPTIONS = List.of(BACKLOG, RECEIVE_BUFFER, SEND_BUFFER, KEEP_ALIVE, READ_TIMEOUT, WRITE_TIMEOUT, MAX_CONNECTIONS, WEB_SOCKET_READ_TIMEOUT, WEB_SOCKET_WRITE_TIMEOUT);
    private static final Collection<AttributeDefinition> SIMPLE_ATTRIBUTES = List.of(SOCKET_BINDING, WORKER, BUFFER_POOL, ENABLED, RESOLVE_PEER_ADDRESS, DISALLOWED_METHODS, SECURE);

    static final Collection<AttributeDefinition> ATTRIBUTES = collectAttributes();

    private static Collection<AttributeDefinition> collectAttributes() {
        Collection<AttributeDefinition> attributes = new ArrayList<>(SIMPLE_ATTRIBUTES.size() + LISTENER_OPTIONS.size() + SOCKET_OPTIONS.size());
        attributes.addAll(SIMPLE_ATTRIBUTES);
        attributes.addAll(LISTENER_OPTIONS);
        attributes.addAll(SOCKET_OPTIONS);
        return Collections.unmodifiableCollection(attributes);
    }

    private final AbstractAddStepHandler addHandler;
    private final Map<AttributeDefinition, OperationStepHandler> writeAttributeHandlers;

    public ListenerResourceDefinition(SimpleResourceDefinition.Parameters parameters, AbstractAddStepHandler addHandler, Map<AttributeDefinition, OperationStepHandler> writeAttributeHandlers) {
        // this Persistent Parameters will be cast to Parameters
        super(parameters.setDescriptionResolver(UndertowExtension.getResolver(Constants.LISTENER)).addCapabilities(LISTENER_CAPABILITY, SERVER_LISTENER_CAPABILITY));
        this.addHandler = addHandler;
        this.writeAttributeHandlers = new HashMap<>();
        this.writeAttributeHandlers.putAll(writeAttributeHandlers);
        this.writeAttributeHandlers.put(ENABLED, new EnabledAttributeHandler());
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerAddOperation(resourceRegistration, this.addHandler, OperationEntry.Flag.RESTART_NONE);
        super.registerRemoveOperation(resourceRegistration, new ServiceRemoveStepHandler(this.addHandler), OperationEntry.Flag.RESTART_NONE);
        resourceRegistration.registerOperationHandler(ResetConnectorStatisticsHandler.DEFINITION, ResetConnectorStatisticsHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attribute : this.getAttributes()) {
            OperationStepHandler writeAttributeHandler = this.writeAttributeHandlers.getOrDefault(attribute, ReloadRequiredWriteAttributeHandler.INSTANCE);
            resourceRegistration.registerReadWriteAttribute(attribute, null, writeAttributeHandler);
        }

        for(ConnectorStat attr : ConnectorStat.values()) {
            resourceRegistration.registerMetric(attr.definition, ReadStatisticHandler.INSTANCE);
        }
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return List.of(new SensitiveTargetAccessConstraintDefinition(new SensitivityClassification(UndertowExtension.SUBSYSTEM_NAME, "web-connector", false, false, false)));
    }

    private static class ReadStatisticHandler implements OperationStepHandler {

        public static final ReadStatisticHandler INSTANCE = new ReadStatisticHandler();

        private ReadStatisticHandler() {

        }

        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            ListenerService service =  getListenerService(context);
            if (service == null) {
                return;
            }
            String op = operation.get(NAME).asString();
            ConnectorStatistics stats = service.getOpenListener().getConnectorStatistics();
            if(stats != null) {
                ConnectorStat element = ConnectorStat.getStat(op);
                switch (element) {
                    case BYTES_RECEIVED:
                        context.getResult().set(stats.getBytesReceived());
                        break;
                    case BYTES_SENT:
                        context.getResult().set(stats.getBytesSent());
                        break;
                    case ERROR_COUNT:
                        context.getResult().set(stats.getErrorCount());
                        break;
                    case MAX_PROCESSING_TIME:
                        context.getResult().set(stats.getMaxProcessingTime());
                        break;
                    case PROCESSING_TIME:
                        context.getResult().set(stats.getProcessingTime());
                        break;
                    case REQUEST_COUNT:
                        context.getResult().set(stats.getRequestCount());
                        break;
                }
            }
        }
    }

    static ListenerService getListenerService(OperationContext context) {
        final String name = context.getCurrentAddressValue();
        ServiceName serviceName = LISTENER_CAPABILITY.getCapabilityServiceName(name);
        ServiceController<ListenerService> listenerSC = (ServiceController<ListenerService>) context.getServiceRegistry(false).getService(serviceName);
        if (listenerSC == null || listenerSC.getState() != ServiceController.State.UP) {
            return null;
        }
        return listenerSC.getValue();
    }

    private static class EnabledAttributeHandler extends AbstractWriteAttributeHandler<Boolean> {

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Boolean> handbackHolder) throws OperationFailedException {

            boolean enabled = resolvedValue.asBoolean();
            // We don't try and analyze currentValue to see if we were already enabled, as the resolution result
            // may be different now than it was before (different system props, or vault contents)
            // Instead we consider the previous setting to be enabled if the service Mode != Mode.NEVER
            ListenerService listenerService = getListenerService(context);
            if (listenerService != null) {
                boolean currentEnabled = listenerService.isEnabled();
                handbackHolder.setHandback(currentEnabled);
                listenerService.setEnabled(enabled);
            }
            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Boolean handback) throws OperationFailedException {
            if (handback != null) {
                ListenerService listenerService = getListenerService(context);
                if (listenerService != null) {
                    listenerService.setEnabled(handback);
                }
            }
        }
    }
}
