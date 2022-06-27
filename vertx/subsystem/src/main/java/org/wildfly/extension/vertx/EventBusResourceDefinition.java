/*
 *  Copyright (c) 2022 The original author or authors
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of Apache License v2.0 which
 *  accompanies this distribution.
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.wildfly.extension.vertx;

import static org.wildfly.extension.vertx.PemTrustOptionsResourceDefinition.PEM_VALUE_MARSHALLER;
import static org.wildfly.extension.vertx.PemTrustOptionsResourceDefinition.PEM_VALUE_PARSER;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.OpenSSLEngineOptions;
import io.vertx.core.net.TrustOptions;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
class EventBusResourceDefinition extends SimpleResourceDefinition implements VertxConstants {

  static final RuntimeCapability<Void> VERTX_EVENT_BUS_OPTIONS_CAPABILITY =
    RuntimeCapability.Builder.of(VertxResourceDefinition.VERTX_CAPABILITY_NAME + ".options.eventbus", true, EventBusOptions.class)
      .build();

  // EventBusOptions
  public static final SimpleAttributeDefinition ATTR_EVENTBUS_SEND_BUFFER_SIZE = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_SEND_BUFFER_SIZE, ModelType.INT)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_RECEIVE_BUFFER_SIZE = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_RECEIVE_BUFFER_SIZE, ModelType.INT)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_TRAFFIC_CLASS = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_TRAFFIC_CLASS, ModelType.INT)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_REUSE_ADDRESS = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_REUSE_ADDRESS, ModelType.BOOLEAN)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_LOG_ACTIVITY = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_LOG_ACTIVITY, ModelType.BOOLEAN)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_REUSE_PORT = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_REUSE_PORT, ModelType.BOOLEAN)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_TCP_NO_DELAY = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_TCP_NO_DELAY, ModelType.BOOLEAN)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_TCP_KEEP_ALIVE = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_TCP_KEEP_ALIVE, ModelType.BOOLEAN)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_SO_LINGER = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_SO_LINGER, ModelType.INT)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_IDLE_TIMEOUT = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_IDLE_TIMEOUT, ModelType.INT)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_READ_IDLE_TIMEOUT = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_READ_IDLE_TIMEOUT, ModelType.INT)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_WRITE_IDLE_TIMEOUT = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_WRITE_IDLE_TIMEOUT, ModelType.INT)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_IDLE_TIMEOUT_UNIT = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_IDLE_TIMEOUT_UNIT, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .setAllowedValues(TIME_UNITS)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_SSL = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_SSL, ModelType.BOOLEAN)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_SSL_HAND_SHAKE_TIMEOUT = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_SSL_HAND_SHAKE_TIMEOUT, ModelType.INT)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_SSL_HAND_SHAKE_TIMEOUT_UNIT = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_SSL_HAND_SHAKE_TIMEOUT_UNIT, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .setAllowedValues(TIME_UNITS)
    .build();

  public static final StringListAttributeDefinition ATTR_EVENTBUS_ENABLED_CIPHER_SUITES = new StringListAttributeDefinition.Builder(VertxConstants.ATTR_EVENTBUS_ENABLED_CIPHER_SUITES)
    .setRequired(false)
    .setElementValidator(new StringLengthValidator(1))
    .setAllowExpression(true)
    .setAttributeParser(AttributeParser.COMMA_DELIMITED_STRING_LIST)
    .setAttributeMarshaller(AttributeMarshaller.COMMA_STRING_LIST)
    .build();

  public static final StringListAttributeDefinition ATTR_EVENTBUS_CRL_PATHS = new StringListAttributeDefinition.Builder(VertxConstants.ATTR_EVENTBUS_CRL_PATHS)
    .setRequired(false)
    .setElementValidator(new StringLengthValidator(1))
    .setAllowExpression(true)
    .setAttributeParser(AttributeParser.COMMA_DELIMITED_STRING_LIST)
    .setAttributeMarshaller(AttributeMarshaller.COMMA_STRING_LIST)
    .build();

  public static final SimpleListAttributeDefinition ATTR_EVENTBUS_CRL_VALUES = new SimpleListAttributeDefinition.Builder(VertxConstants.ATTR_EVENTBUS_CRL_VALUES, PemTrustOptionsResourceDefinition.ATTR_PEM_VALUE)
    .setRequired(false)
    .setElementValidator(new StringLengthValidator(1))
    .setAllowExpression(true)
    .setAttributeParser(PEM_VALUE_PARSER)
    .setAttributeMarshaller(PEM_VALUE_MARSHALLER)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_USE_ALPN = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_USE_ALPN, ModelType.BOOLEAN)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final StringListAttributeDefinition ATTR_EVENTBUS_ENABLED_SECURE_TRANSPORT_PROTOCOLS = new StringListAttributeDefinition.Builder(VertxConstants.ATTR_EVENTBUS_ENABLED_SECURE_TRANSPORT_PROTOCOLS)
    .setRequired(false)
    .setElementValidator(new StringLengthValidator(1))
    .setAllowExpression(true)
    .setAttributeParser(AttributeParser.COMMA_DELIMITED_STRING_LIST)
    .setAttributeMarshaller(AttributeMarshaller.COMMA_STRING_LIST)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_TCP_FAST_OPEN = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_TCP_FAST_OPEN, ModelType.BOOLEAN)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_TCP_CORK = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_TCP_CORK, ModelType.BOOLEAN)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_TCP_QUICK_ACK = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_TCP_QUICK_ACK, ModelType.BOOLEAN)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_SSL_ENGINE_TYPE = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_SSL_ENGINE_TYPE, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .setAllowedValues(SSL_ENGINE_TYPES)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_OPENSSL_SESSION_CACHE_ENABLED = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_OPENSSL_SESSION_CACHE_ENABLED, ModelType.BOOLEAN)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_CLUSTER_PUBLIC_HOST = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_CLUSTER_PUBLIC_HOST, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_CLUSTER_PUBLIC_PORT = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_CLUSTER_PUBLIC_PORT, ModelType.INT)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_CLUSTER_PING_INTERVAL = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_CLUSTER_PING_INTERVAL, ModelType.LONG)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_CLUSTER_PING_REPLY_INTERVAL = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_CLUSTER_PING_REPLY_INTERVAL, ModelType.LONG)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_HOST = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_HOST, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_PORT = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_PORT, ModelType.INT)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_ACCEPT_BACKLOG = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_ACCEPT_BACKLOG, ModelType.INT)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_CLIENT_AUTH = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_CLIENT_AUTH, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .setAllowedValues(CLIENT_AUTHS)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_RECONNECT_ATTEMPTS = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_RECONNECT_ATTEMPTS, ModelType.INT)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_RECONNECT_INTERVAL = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_RECONNECT_INTERVAL, ModelType.LONG)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_CONNECT_TIMEOUT = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_CONNECT_TIMEOUT, ModelType.INT)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_TRUST_ALL = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_TRUST_ALL, ModelType.BOOLEAN)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_KEY_CERT_OPTION = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_KEY_CERT_OPTION, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .setCapabilityReference(KeyStoreOptionsResourceDefinition.KEY_CERT_OPTIONS_CAPABILITY.getName())
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_TRUST_OPTION = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_TRUST_OPTION, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .setCapabilityReference(KeyStoreOptionsResourceDefinition.TRUST_OPTIONS_CAPABILITY.getName())
    .build();

  public static final SimpleAttributeDefinition ATTR_EVENTBUS_CLUSTER_NODE_METADATA = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTBUS_CLUSTER_NODE_METADATA, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .setCapabilityReference(ClusterNodeMetadataResourceDefinition.VERTX_CLUSTER_NODE_METADATA_CAPABILITY.getName())
    .build();

  private static final List<AttributeDefinition> VERTX_EVENTBUS_ATTRS = new ArrayList<>();
  static {
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_SEND_BUFFER_SIZE);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_RECEIVE_BUFFER_SIZE);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_TRAFFIC_CLASS);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_REUSE_ADDRESS);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_LOG_ACTIVITY);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_REUSE_PORT);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_TCP_NO_DELAY);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_TCP_KEEP_ALIVE);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_SO_LINGER);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_IDLE_TIMEOUT);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_READ_IDLE_TIMEOUT);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_WRITE_IDLE_TIMEOUT);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_IDLE_TIMEOUT_UNIT);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_SSL);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_SSL_HAND_SHAKE_TIMEOUT);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_SSL_HAND_SHAKE_TIMEOUT_UNIT);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_ENABLED_CIPHER_SUITES);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_CRL_PATHS);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_CRL_VALUES);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_USE_ALPN);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_ENABLED_SECURE_TRANSPORT_PROTOCOLS);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_TCP_FAST_OPEN);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_TCP_CORK);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_TCP_QUICK_ACK);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_SSL_ENGINE_TYPE);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_OPENSSL_SESSION_CACHE_ENABLED);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_CLUSTER_PUBLIC_HOST);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_CLUSTER_PUBLIC_PORT);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_CLUSTER_PING_INTERVAL);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_CLUSTER_PING_REPLY_INTERVAL);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_HOST);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_PORT);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_ACCEPT_BACKLOG);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_CLIENT_AUTH);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_RECONNECT_ATTEMPTS);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_RECONNECT_INTERVAL);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_CONNECT_TIMEOUT);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_TRUST_ALL);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_KEY_CERT_OPTION);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_TRUST_OPTION);
    VERTX_EVENTBUS_ATTRS.add(ATTR_EVENTBUS_CLUSTER_NODE_METADATA);
  }

  static List<AttributeDefinition> getVertxEventbusAttrs() {
    return VERTX_EVENTBUS_ATTRS;
  }

  static EventBusResourceDefinition INSTANCE = new EventBusResourceDefinition();

  EventBusResourceDefinition() {
    super(new SimpleResourceDefinition.Parameters(PathElement.pathElement(ELEMENT_VERTX_EVENTBUS),
      VertxSubsystemExtension.getResourceDescriptionResolver(VertxSubsystemExtension.SUBSYSTEM_NAME, ELEMENT_VERTX_EVENTBUS))
      .setAddHandler(new AddEventBusOptionsHandler())
      .setRemoveHandler(new RemoveEventBusHandler())
      .setCapabilities(VERTX_EVENT_BUS_OPTIONS_CAPABILITY)
    );
  }

  @Override
  public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
    super.registerAttributes(resourceRegistration);
    AbstractVertxOptionsResourceDefinition.AttrWriteHandler handler = new AbstractVertxOptionsResourceDefinition.AttrWriteHandler(getVertxEventbusAttrs()) {
      @Override
      protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        return AbstractVertxOptionsResourceDefinition.isEventBusUsed(context, context.getCurrentAddressValue());
      }
    };
    for (AttributeDefinition attr : getVertxEventbusAttrs()) {
      resourceRegistration.registerReadWriteAttribute(attr, null, handler);
    }
  }

  private static class RemoveEventBusHandler extends AbstractRemoveStepHandler {
    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
      final String name = context.getCurrentAddressValue();
      boolean needsReload = AbstractVertxOptionsResourceDefinition.isEventBusUsed(context, name);
      ServiceName serviceName = VERTX_EVENT_BUS_OPTIONS_CAPABILITY.getCapabilityServiceName(name);
      context.removeService(serviceName);
      if (needsReload) {
        context.reloadRequired();
      }
    }
  }

  private static class AddEventBusOptionsHandler extends AbstractAddStepHandler {
    AddEventBusOptionsHandler() {
      super(new Parameters().addAttribute(VERTX_EVENTBUS_ATTRS));
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
      final String name = context.getCurrentAddressValue();
      EventBusOptions eventBusOptions = parseEventBusOptions(operation);
      ServiceName serviceName = VERTX_EVENT_BUS_OPTIONS_CAPABILITY.getCapabilityServiceName(name);
      ServiceBuilder<?> builder = context.getServiceTarget().addService(serviceName);
      Supplier<JsonObject> clusterNodeMetaSupplier = null;
      if (operation.hasDefined(ATTR_EVENTBUS_CLUSTER_NODE_METADATA.getName())) {
        String clusterNodeMetaName = ATTR_EVENTBUS_CLUSTER_NODE_METADATA.validateOperation(operation).asString();
        clusterNodeMetaSupplier = builder.requires(ClusterNodeMetadataResourceDefinition.VERTX_CLUSTER_NODE_METADATA_CAPABILITY.getCapabilityServiceName(clusterNodeMetaName));
      }
      Supplier<KeyCertOptions> keyCertOptionsSupplier = null;
      if (operation.hasDefined(ATTR_EVENTBUS_KEY_CERT_OPTION.getName())) {
        String keyCertOptionName = ATTR_EVENTBUS_KEY_CERT_OPTION.validateOperation(operation).asString();
        keyCertOptionsSupplier = builder.requires(KeyStoreOptionsResourceDefinition.KEY_CERT_OPTIONS_CAPABILITY.getCapabilityServiceName(keyCertOptionName));
      }
      Supplier<TrustOptions> trustOptionsSupplier = null;
      if (operation.hasDefined(ATTR_EVENTBUS_TRUST_OPTION.getName())) {
        String trustOptionName = ATTR_EVENTBUS_TRUST_OPTION.validateOperation(operation).asString();
        trustOptionsSupplier = builder.requires(KeyStoreOptionsResourceDefinition.TRUST_OPTIONS_CAPABILITY.getCapabilityServiceName(trustOptionName));
      }
      final Supplier<JsonObject> theClusterNodeMeta = clusterNodeMetaSupplier;
      final Supplier<KeyCertOptions> theKeyCertOptions = keyCertOptionsSupplier;
      final Supplier<TrustOptions> theTrustOptions = trustOptionsSupplier;
      final Consumer<EventBusOptions> consumer = builder.provides(serviceName);
      Service eventBusOptionService = new Service() {

        @Override
        public void start(StartContext startContext) throws StartException {
          VertxOptionsRegistry.getInstance().addEventBusOptions(name, eventBusOptions);
          if (theClusterNodeMeta != null && theClusterNodeMeta.get() != null) {
            eventBusOptions.setClusterNodeMetadata(theClusterNodeMeta.get());
          }
          if (theKeyCertOptions != null && theKeyCertOptions.get() != null) {
            eventBusOptions.setKeyCertOptions(theKeyCertOptions.get());
          }
          if (theTrustOptions != null && theTrustOptions.get() != null) {
            eventBusOptions.setTrustOptions(theTrustOptions.get());
          }
          consumer.accept(eventBusOptions);
        }

        @Override
        public void stop(StopContext stopContext) {
          eventBusOptions.setClusterNodeMetadata(null);
          eventBusOptions.setKeyCertOptions(null);
          eventBusOptions.setTrustOptions(null);
          VertxOptionsRegistry.getInstance().removeEventBusOptions(name);
        }
      };
      builder.setInstance(eventBusOptionService)
        .setInitialMode(ServiceController.Mode.ON_DEMAND)
        .install();
    }

    private EventBusOptions parseEventBusOptions(ModelNode operation) throws OperationFailedException {
      EventBusOptions eventBusOptions = new EventBusOptions();
      if (operation.hasDefined(ATTR_EVENTBUS_SEND_BUFFER_SIZE.getName())) {
        eventBusOptions.setSendBufferSize(ATTR_EVENTBUS_SEND_BUFFER_SIZE.validateOperation(operation).asInt());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_RECEIVE_BUFFER_SIZE.getName())) {
        eventBusOptions.setReceiveBufferSize(ATTR_EVENTBUS_RECEIVE_BUFFER_SIZE.validateOperation(operation).asInt());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_TRAFFIC_CLASS.getName())) {
        eventBusOptions.setTrafficClass(ATTR_EVENTBUS_TRAFFIC_CLASS.validateOperation(operation).asInt());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_REUSE_ADDRESS.getName())) {
        eventBusOptions.setReuseAddress(ATTR_EVENTBUS_REUSE_ADDRESS.validateOperation(operation).asBoolean());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_LOG_ACTIVITY.getName())) {
        eventBusOptions.setLogActivity(ATTR_EVENTBUS_LOG_ACTIVITY.validateOperation(operation).asBoolean());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_REUSE_PORT.getName())) {
        eventBusOptions.setReusePort(ATTR_EVENTBUS_REUSE_PORT.validateOperation(operation).asBoolean());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_TCP_NO_DELAY.getName())) {
        eventBusOptions.setTcpNoDelay(ATTR_EVENTBUS_TCP_NO_DELAY.validateOperation(operation).asBoolean());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_TCP_KEEP_ALIVE.getName())) {
        eventBusOptions.setTcpKeepAlive(ATTR_EVENTBUS_TCP_KEEP_ALIVE.validateOperation(operation).asBoolean());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_SO_LINGER.getName())) {
        eventBusOptions.setSoLinger(ATTR_EVENTBUS_SO_LINGER.validateOperation(operation).asInt());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_IDLE_TIMEOUT.getName())) {
        eventBusOptions.setIdleTimeout(ATTR_EVENTBUS_IDLE_TIMEOUT.validateOperation(operation).asInt());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_READ_IDLE_TIMEOUT.getName())) {
        eventBusOptions.setReadIdleTimeout(ATTR_EVENTBUS_READ_IDLE_TIMEOUT.validateOperation(operation).asInt());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_WRITE_IDLE_TIMEOUT.getName())) {
        eventBusOptions.setWriteIdleTimeout(ATTR_EVENTBUS_WRITE_IDLE_TIMEOUT.validateOperation(operation).asInt());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_IDLE_TIMEOUT_UNIT.getName())) {
        eventBusOptions.setIdleTimeoutUnit(TimeUnit.valueOf(ATTR_EVENTBUS_IDLE_TIMEOUT_UNIT.validateOperation(operation).asString()));
      }
      if (operation.hasDefined(ATTR_EVENTBUS_SSL.getName())) {
        eventBusOptions.setSsl(ATTR_EVENTBUS_SSL.validateOperation(operation).asBoolean());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_SSL_HAND_SHAKE_TIMEOUT.getName())) {
        eventBusOptions.setSslHandshakeTimeout(ATTR_EVENTBUS_SSL_HAND_SHAKE_TIMEOUT.validateOperation(operation).asInt());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_SSL_HAND_SHAKE_TIMEOUT_UNIT.getName())) {
        eventBusOptions.setSslHandshakeTimeoutUnit(TimeUnit.valueOf(ATTR_EVENTBUS_SSL_HAND_SHAKE_TIMEOUT_UNIT.validateOperation(operation).asString()));
      }
      if (operation.hasDefined(ATTR_EVENTBUS_ENABLED_CIPHER_SUITES.getName())) {
        eventBusOptions.getEnabledCipherSuites().clear();
        ATTR_EVENTBUS_ENABLED_CIPHER_SUITES.validateOperation(operation).asList().forEach(e -> eventBusOptions.addEnabledCipherSuite(e.asString()));
      }
      if (operation.hasDefined(ATTR_EVENTBUS_CRL_PATHS.getName())) {
        eventBusOptions.getCrlPaths().clear();
        ATTR_EVENTBUS_CRL_PATHS.validateOperation(operation).asList().forEach(e -> eventBusOptions.addCrlPath(e.asString()));
      }
      if (operation.hasDefined(ATTR_EVENTBUS_CRL_VALUES.getName())) {
        eventBusOptions.getCrlValues().clear();
        ATTR_EVENTBUS_CRL_VALUES.validateOperation(operation).asList().forEach(e -> eventBusOptions.addCrlValue(Buffer.buffer(e.asString())));
      }
      if (operation.hasDefined(ATTR_EVENTBUS_USE_ALPN.getName())) {
        eventBusOptions.setUseAlpn(ATTR_EVENTBUS_USE_ALPN.validateOperation(operation).asBoolean());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_ENABLED_SECURE_TRANSPORT_PROTOCOLS.getName())) {
        eventBusOptions.getEnabledSecureTransportProtocols().clear();
        ATTR_EVENTBUS_ENABLED_SECURE_TRANSPORT_PROTOCOLS.validateOperation(operation).asList().forEach(e -> eventBusOptions.addEnabledSecureTransportProtocol(e.asString()));
      }
      if (operation.hasDefined(ATTR_EVENTBUS_TCP_FAST_OPEN.getName())) {
        eventBusOptions.setTcpFastOpen(ATTR_EVENTBUS_TCP_FAST_OPEN.validateOperation(operation).asBoolean());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_TCP_CORK.getName())) {
        eventBusOptions.setTcpCork(ATTR_EVENTBUS_TCP_CORK.validateOperation(operation).asBoolean());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_TCP_QUICK_ACK.getName())) {
        eventBusOptions.setTcpQuickAck(ATTR_EVENTBUS_TCP_QUICK_ACK.validateOperation(operation).asBoolean());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_SSL_ENGINE_TYPE.getName())) {
        SSL_ENGINE_TYPE engineType = SSL_ENGINE_TYPE.valueOf(ATTR_EVENTBUS_SSL_ENGINE_TYPE.validateOperation(operation).asString());
        if (SSL_ENGINE_TYPE.OPENSSL.equals(engineType)) {
          OpenSSLEngineOptions openSSLEngine = new OpenSSLEngineOptions();
          if (operation.hasDefined(ATTR_EVENTBUS_OPENSSL_SESSION_CACHE_ENABLED.getName())) {
            openSSLEngine.setSessionCacheEnabled(ATTR_EVENTBUS_OPENSSL_SESSION_CACHE_ENABLED.validateOperation(operation).asBoolean());
          }
          eventBusOptions.setSslEngineOptions(openSSLEngine);
        }
      }
      if (operation.hasDefined(ATTR_EVENTBUS_CLUSTER_PUBLIC_HOST.getName())) {
        eventBusOptions.setClusterPublicHost(ATTR_EVENTBUS_CLUSTER_PUBLIC_HOST.validateOperation(operation).asString());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_CLUSTER_PUBLIC_PORT.getName())) {
        eventBusOptions.setClusterPublicPort(ATTR_EVENTBUS_CLUSTER_PUBLIC_PORT.validateOperation(operation).asInt());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_CLUSTER_PING_INTERVAL.getName())) {
        eventBusOptions.setClusterPingInterval(ATTR_EVENTBUS_CLUSTER_PING_INTERVAL.validateOperation(operation).asLong());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_CLUSTER_PING_REPLY_INTERVAL.getName())) {
        eventBusOptions.setClusterPingReplyInterval(ATTR_EVENTBUS_CLUSTER_PING_REPLY_INTERVAL.validateOperation(operation).asLong());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_HOST.getName())) {
        eventBusOptions.setHost(ATTR_EVENTBUS_HOST.validateOperation(operation).asString());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_PORT.getName())) {
        eventBusOptions.setPort(ATTR_EVENTBUS_PORT.validateOperation(operation).asInt());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_ACCEPT_BACKLOG.getName())) {
        eventBusOptions.setAcceptBacklog(ATTR_EVENTBUS_ACCEPT_BACKLOG.validateOperation(operation).asInt());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_CLIENT_AUTH.getName())) {
        eventBusOptions.setClientAuth(ClientAuth.valueOf(ATTR_EVENTBUS_CLIENT_AUTH.validateOperation(operation).asString()));
      }
      if (operation.hasDefined(ATTR_EVENTBUS_RECONNECT_ATTEMPTS.getName())) {
        eventBusOptions.setReconnectAttempts(ATTR_EVENTBUS_RECONNECT_ATTEMPTS.validateOperation(operation).asInt());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_RECONNECT_INTERVAL.getName())) {
        eventBusOptions.setReconnectInterval(ATTR_EVENTBUS_RECONNECT_INTERVAL.validateOperation(operation).asLong());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_CONNECT_TIMEOUT.getName())) {
        eventBusOptions.setConnectTimeout(ATTR_EVENTBUS_CONNECT_TIMEOUT.validateOperation(operation).asInt());
      }
      if (operation.hasDefined(ATTR_EVENTBUS_TRUST_ALL.getName())) {
        eventBusOptions.setTrustAll(ATTR_EVENTBUS_TRUST_ALL.validateOperation(operation).asBoolean());
      }
      return eventBusOptions;
    }

  }

}
