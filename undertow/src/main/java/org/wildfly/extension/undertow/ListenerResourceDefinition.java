/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import io.undertow.UndertowOptions;
import io.undertow.server.ConnectorStatistics;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.io.OptionAttributeDefinition;
import org.xnio.Options;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 * @author Stuart Douglas
 */
abstract class ListenerResourceDefinition extends PersistentResourceDefinition {

    static final String IO_WORKER_CAPABILITY = "org.wildfly.io.worker";
    static final String IO_BUFFER_POOL_CAPABILITY = "org.wildfly.io.buffer-pool";
    static final String SOCKET_CAPABILITY = "org.wildfly.network.socket-binding";

    protected static final String LISTENER_CAPABILITY_NAME = "org.wildfly.undertow.listener";
    static final RuntimeCapability<Void> LISTENER_CAPABILITY = RuntimeCapability.Builder.of(LISTENER_CAPABILITY_NAME, true, ListenerService.class)
            .build();

    protected static final SimpleAttributeDefinition SOCKET_BINDING = new SimpleAttributeDefinitionBuilder(Constants.SOCKET_BINDING, ModelType.STRING)
            .setAllowNull(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1))
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .setCapabilityReference(SOCKET_CAPABILITY, LISTENER_CAPABILITY)
            .build();
    protected static final SimpleAttributeDefinition WORKER = new SimpleAttributeDefinitionBuilder(Constants.WORKER, ModelType.STRING)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1))
            .setDefaultValue(new ModelNode("default"))
            .setCapabilityReference(IO_WORKER_CAPABILITY, LISTENER_CAPABILITY)
            .build();
    protected static final SimpleAttributeDefinition BUFFER_POOL = new SimpleAttributeDefinitionBuilder(Constants.BUFFER_POOL, ModelType.STRING)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1))
            .setDefaultValue(new ModelNode("default"))
            .setCapabilityReference(IO_BUFFER_POOL_CAPABILITY, LISTENER_CAPABILITY)
            .build();
    protected static final SimpleAttributeDefinition ENABLED = new SimpleAttributeDefinitionBuilder(Constants.ENABLED, ModelType.BOOLEAN)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(true))
            .setAllowExpression(true)
            .build();

    protected static final SimpleAttributeDefinition REDIRECT_SOCKET = new SimpleAttributeDefinitionBuilder(Constants.REDIRECT_SOCKET, ModelType.STRING)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setAllowExpression(false)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    protected static final SimpleAttributeDefinition RESOLVE_PEER_ADDRESS = new SimpleAttributeDefinitionBuilder(Constants.RESOLVE_PEER_ADDRESS, ModelType.BOOLEAN)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setDefaultValue(new ModelNode(false))
            .setAllowExpression(true)
            .build();

    protected static final StringListAttributeDefinition DISALLOWED_METHODS = new StringListAttributeDefinition.Builder(Constants.DISALLOWED_METHODS)
            .setDefaultValue(new ModelNode().add("TRACE"))
            .setAllowNull(true)
            .setValidator(new StringLengthValidator(0))
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setAllowExpression(true)
            .build();

    protected static final SimpleAttributeDefinition SECURE = new SimpleAttributeDefinitionBuilder(Constants.SECURE, ModelType.BOOLEAN)
            .setDefaultValue(new ModelNode(false))
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setAllowExpression(true)
            .build();

    public static final OptionAttributeDefinition BACKLOG = OptionAttributeDefinition.builder("tcp-backlog", Options.BACKLOG).setDefaultValue(new ModelNode(10000)).setAllowExpression(true).build();
    public static final OptionAttributeDefinition RECEIVE_BUFFER = OptionAttributeDefinition.builder("receive-buffer", Options.RECEIVE_BUFFER).setAllowExpression(true).build();
    public static final OptionAttributeDefinition SEND_BUFFER = OptionAttributeDefinition.builder("send-buffer", Options.SEND_BUFFER).setAllowExpression(true).build();
    public static final OptionAttributeDefinition KEEP_ALIVE = OptionAttributeDefinition.builder("tcp-keep-alive", Options.KEEP_ALIVE).setAllowExpression(true).build();
    public static final OptionAttributeDefinition READ_TIMEOUT = OptionAttributeDefinition.builder("read-timeout", Options.READ_TIMEOUT).setAllowExpression(true).build();
    public static final OptionAttributeDefinition WRITE_TIMEOUT = OptionAttributeDefinition.builder("write-timeout", Options.WRITE_TIMEOUT).setAllowExpression(true).build();
    public static final OptionAttributeDefinition MAX_CONNECTIONS = OptionAttributeDefinition.builder(Constants.MAX_CONNECTIONS, Options.CONNECTION_HIGH_WATER).setAllowExpression(true).build();


    public static final OptionAttributeDefinition MAX_HEADER_SIZE = OptionAttributeDefinition.builder("max-header-size", UndertowOptions.MAX_HEADER_SIZE).setDefaultValue(new ModelNode(UndertowOptions.DEFAULT_MAX_HEADER_SIZE)).setAllowExpression(true).build();
    public static final OptionAttributeDefinition MAX_ENTITY_SIZE = OptionAttributeDefinition.builder(Constants.MAX_POST_SIZE, UndertowOptions.MAX_ENTITY_SIZE).setDefaultValue(new ModelNode(10485760L)).setAllowExpression(true).build();
    public static final OptionAttributeDefinition BUFFER_PIPELINED_DATA = OptionAttributeDefinition.builder("buffer-pipelined-data", UndertowOptions.BUFFER_PIPELINED_DATA).setDefaultValue(new ModelNode(false)).setAllowExpression(true).build();
    public static final OptionAttributeDefinition MAX_PARAMETERS = OptionAttributeDefinition.builder("max-parameters", UndertowOptions.MAX_PARAMETERS).setDefaultValue(new ModelNode(1000)).setAllowExpression(true).build();
    public static final OptionAttributeDefinition MAX_HEADERS = OptionAttributeDefinition.builder("max-headers", UndertowOptions.MAX_HEADERS).setDefaultValue(new ModelNode(200)).setAllowExpression(true).build();
    public static final OptionAttributeDefinition MAX_COOKIES = OptionAttributeDefinition.builder("max-cookies", UndertowOptions.MAX_COOKIES).setDefaultValue(new ModelNode(200)).setAllowExpression(true).build();
    public static final OptionAttributeDefinition ALLOW_ENCODED_SLASH = OptionAttributeDefinition.builder("allow-encoded-slash", UndertowOptions.ALLOW_ENCODED_SLASH).setDefaultValue(new ModelNode(false)).setAllowExpression(true).build();
    public static final OptionAttributeDefinition DECODE_URL = OptionAttributeDefinition.builder("decode-url", UndertowOptions.DECODE_URL).setDefaultValue(new ModelNode(true)).setAllowExpression(true).build();
    public static final OptionAttributeDefinition URL_CHARSET = OptionAttributeDefinition.builder("url-charset", UndertowOptions.URL_CHARSET).setDefaultValue(new ModelNode("UTF-8")).setAllowExpression(true).build();
    public static final OptionAttributeDefinition ALWAYS_SET_KEEP_ALIVE = OptionAttributeDefinition.builder("always-set-keep-alive", UndertowOptions.ALWAYS_SET_KEEP_ALIVE).setDefaultValue(new ModelNode(true)).setAllowExpression(true).build();
    public static final OptionAttributeDefinition MAX_BUFFERED_REQUEST_SIZE = OptionAttributeDefinition.builder(Constants.MAX_BUFFERED_REQUEST_SIZE, UndertowOptions.MAX_BUFFERED_REQUEST_SIZE).setDefaultValue(new ModelNode(16384)).setAllowExpression(true).build();
    public static final OptionAttributeDefinition RECORD_REQUEST_START_TIME = OptionAttributeDefinition.builder("record-request-start-time", UndertowOptions.RECORD_REQUEST_START_TIME).setDefaultValue(new ModelNode(false)).setAllowExpression(true).build();
    public static final OptionAttributeDefinition ALLOW_EQUALS_IN_COOKIE_VALUE = OptionAttributeDefinition.builder("allow-equals-in-cookie-value", UndertowOptions.ALLOW_EQUALS_IN_COOKIE_VALUE).setDefaultValue(new ModelNode(false)).setAllowExpression(true).build();
    public static final OptionAttributeDefinition NO_REQUEST_TIMEOUT = OptionAttributeDefinition.builder("no-request-timeout", UndertowOptions.NO_REQUEST_TIMEOUT).setDefaultValue(new ModelNode(60000)).setMeasurementUnit(MeasurementUnit.MILLISECONDS).setAllowNull(true).setAllowExpression(true).build();
    public static final OptionAttributeDefinition REQUEST_PARSE_TIMEOUT = OptionAttributeDefinition.builder("request-parse-timeout", UndertowOptions.REQUEST_PARSE_TIMEOUT).setMeasurementUnit(MeasurementUnit.MILLISECONDS).setAllowNull(true).setAllowExpression(true).build();

    public enum ConnectorStat {
        REQUEST_COUNT(new SimpleAttributeDefinitionBuilder("request-count", ModelType.LONG)
                .setUndefinedMetricValue(new ModelNode(0)).setStorageRuntime().build()),
        BYTES_SENT(new SimpleAttributeDefinitionBuilder("bytes-sent", ModelType.LONG)
                .setUndefinedMetricValue(new ModelNode(0)).setStorageRuntime().build()),
        BYTES_RECEIVED(new SimpleAttributeDefinitionBuilder("bytes-received", ModelType.LONG)
                .setUndefinedMetricValue(new ModelNode(0)).setStorageRuntime().build()),
        ERROR_COUNT(new SimpleAttributeDefinitionBuilder("error-count", ModelType.LONG)
                .setUndefinedMetricValue(new ModelNode(0)).setStorageRuntime().build()),
        PROCESSING_TIME(new SimpleAttributeDefinitionBuilder("processing-time", ModelType.LONG)
                .setUndefinedMetricValue(new ModelNode(0)).setStorageRuntime().build()),
        MAX_PROCESSING_TIME(new SimpleAttributeDefinitionBuilder("max-processing-time", ModelType.LONG)
                .setUndefinedMetricValue(new ModelNode(0)).setStorageRuntime().build());

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

    protected static final Collection ATTRIBUTES;
    protected static final List<AccessConstraintDefinition> CONSTRAINTS = Arrays.asList(UndertowExtension.LISTENER_CONSTRAINT);

    public static final List<OptionAttributeDefinition> LISTENER_OPTIONS = Arrays.asList(MAX_HEADER_SIZE, MAX_ENTITY_SIZE,
            BUFFER_PIPELINED_DATA, MAX_PARAMETERS, MAX_HEADERS, MAX_COOKIES, ALLOW_ENCODED_SLASH, DECODE_URL,
            URL_CHARSET, ALWAYS_SET_KEEP_ALIVE, MAX_BUFFERED_REQUEST_SIZE, RECORD_REQUEST_START_TIME,
            ALLOW_EQUALS_IN_COOKIE_VALUE, NO_REQUEST_TIMEOUT, REQUEST_PARSE_TIMEOUT);

    public static final List<OptionAttributeDefinition> SOCKET_OPTIONS = Arrays.asList(BACKLOG, RECEIVE_BUFFER, SEND_BUFFER, KEEP_ALIVE, READ_TIMEOUT, WRITE_TIMEOUT, MAX_CONNECTIONS);

    static {
        ATTRIBUTES = new LinkedHashSet<AttributeDefinition>(Arrays.asList(SOCKET_BINDING, WORKER, BUFFER_POOL, ENABLED, RESOLVE_PEER_ADDRESS, DISALLOWED_METHODS, SECURE));
        ATTRIBUTES.addAll(LISTENER_OPTIONS);
        ATTRIBUTES.addAll(SOCKET_OPTIONS);
    }

    public ListenerResourceDefinition(PathElement pathElement) {
        super(pathElement, UndertowExtension.getResolver(Constants.LISTENER)
        );
    }

    public Collection<AttributeDefinition> getAttributes() {
        //noinspection unchecked
        return ATTRIBUTES;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        super.registerAddOperation(resourceRegistration, getAddHandler(), OperationEntry.Flag.RESTART_NONE);
        super.registerRemoveOperation(resourceRegistration, new ListenerRemoveHandler(getAddHandler()), OperationEntry.Flag.RESTART_NONE);
        resourceRegistration.registerOperationHandler(ResetConnectorStatisticsHandler.DEFINITION, ResetConnectorStatisticsHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        // DO NOT call super, as we need non-standard handling for enabled

        Collection<AttributeDefinition> ads = getAttributes();
        OperationStepHandler rrh = new ReloadRequiredWriteAttributeHandler(ads); // we include ENABLED in this set, but it doesn't matter we don't register rrh for it
        OperationStepHandler enh = new EnabledAttributeHandler();
        for (AttributeDefinition ad : ads) {
            OperationStepHandler osh = ad == ENABLED ? enh : rrh;
            resourceRegistration.registerReadWriteAttribute(ad, null, osh);
        }

        for(ConnectorStat attr : ConnectorStat.values()) {
            resourceRegistration.registerMetric(attr.definition, ReadStatisticHandler.INSTANCE);
        }
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return CONSTRAINTS;
    }

    protected abstract ListenerAdd getAddHandler();


    private static class ReadStatisticHandler implements OperationStepHandler {

        public static final ReadStatisticHandler INSTANCE = new ReadStatisticHandler();

        private ReadStatisticHandler() {

        }

        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final String name = context.getCurrentAddressValue();
            ServiceController<ListenerService> listenerSC = (ServiceController<ListenerService>) context.getServiceRegistry(false).getService(UndertowService.listenerName(name));
            if (listenerSC ==null || listenerSC.getState() != ServiceController.State.UP){
                context.getResult().set(0L);
                return;
            }
            String op = operation.get(NAME).asString();
            ListenerService<?> service = listenerSC.getValue();
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
            } else {
                context.getResult().set(0L);
            }
            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }
    }

    @Override
    public void registerCapabilities(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerCapability(LISTENER_CAPABILITY);
    }

    private static class EnabledAttributeHandler extends AbstractWriteAttributeHandler<Boolean> {
        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Boolean> handbackHolder) throws OperationFailedException {

            final ServiceName listenerServiceName = UndertowService.listenerName(context.getCurrentAddressValue());

            boolean enabled = resolvedValue.asBoolean();
            // We don't try and analyze currentValue to see if we were already enabled, as the resolution result
            // may be different now than it was before (different system props, or vault contents)
            // Instead we consider the previous setting to be enabled if the service Mode != Mode.NEVER
            final ServiceController<?> controller = context.getServiceRegistry(true).getRequiredService(listenerServiceName);
            boolean currentEnabled = controller.getMode() != ServiceController.Mode.NEVER;

            if (enabled) {
                if (!currentEnabled) {
                    // It's safe to enable
                    controller.setMode(ServiceController.Mode.ACTIVE);
                }
                // Pass the current setting into the handback so it knows whether or not to revert anything
                handbackHolder.setHandback(currentEnabled);
                return false;
            } else if (currentEnabled) {
                // going from enabled to disabled requires reload
                return true;
            } else {
                // tell revertUpdateToRuntime it doesn't need to revert anything since we did nothing
                handbackHolder.setHandback(true);
                return false;
            }
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Boolean handback) throws OperationFailedException {
            if (handback != null && !handback) {
                // applyUpdateToRuntime changed from disabled to enabled
                final ServiceName listenerServiceName = UndertowService.listenerName(context.getCurrentAddressValue());
                context.getServiceRegistry(true).getRequiredService(listenerServiceName).setMode(ServiceController.Mode.NEVER);
            } // else applyUpdateToRuntime did nothing as the service was already in the desired state
              // So we do nothing as well
        }
    }
}
