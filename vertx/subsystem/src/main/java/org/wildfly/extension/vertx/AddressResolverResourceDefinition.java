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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
import io.vertx.core.dns.AddressResolverOptions;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
class AddressResolverResourceDefinition extends SimpleResourceDefinition implements VertxConstants {

  static final RuntimeCapability<Void> VERTX_OPTIONS_ADDRESS_RESOLVER_CAPABILITY =
    RuntimeCapability.Builder.of(VertxResourceDefinition.VERTX_CAPABILITY_NAME + ".options.address.resolver", true, AddressResolverOptions.class)
      .build();

  // AddressResolverOptions
  public static final SimpleAttributeDefinition ATTR_HOSTS_PATH = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_HOSTS_PATH, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_HOSTS_VALUE = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_HOSTS_VALUE, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final StringListAttributeDefinition ATTR_SERVERS = new StringListAttributeDefinition.Builder(VertxConstants.ATTR_SERVERS)
    .setRequired(false)
    .setElementValidator(new StringLengthValidator(1))
    .setAllowExpression(true)
    .setAttributeParser(AttributeParser.COMMA_DELIMITED_STRING_LIST)
    .setAttributeMarshaller(AttributeMarshaller.COMMA_STRING_LIST)
    .build();

  public static final SimpleAttributeDefinition ATTR_OPT_RES_ENABLED = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_OPT_RES_ENABLED, ModelType.BOOLEAN)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_CACHE_MIN_TTL = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_CACHE_MIN_TTL, ModelType.INT)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_MAX_TTL = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_MAX_TTL, ModelType.INT)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_NEGATIVE_TTL = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_NEGATIVE_TTL, ModelType.INT)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_QUERY_TIMEOUT = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_QUERY_TIMEOUT, ModelType.LONG)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_MAX_QUERIES = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_MAX_QUERIES, ModelType.INT)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_RD_FLAG = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_RD_FLAG, ModelType.BOOLEAN)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final StringListAttributeDefinition ATTR_SEARCH_DOMAIN = new StringListAttributeDefinition.Builder(VertxConstants.ATTR_SEARCH_DOMAIN)
    .setRequired(false)
    .setElementValidator(new StringLengthValidator(1))
    .setAllowExpression(true)
    .setAttributeParser(AttributeParser.COMMA_DELIMITED_STRING_LIST)
    .setAttributeMarshaller(AttributeMarshaller.COMMA_STRING_LIST)
    .build();

  public static final SimpleAttributeDefinition ATTR_N_DOTS = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_N_DOTS, ModelType.INT)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_ROTATE_SERVERS = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_ROTATE_SERVERS, ModelType.BOOLEAN)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_ROUND_ROBIN_INET_ADDRESS = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_ROUND_ROBIN_INET_ADDRESS, ModelType.BOOLEAN)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  private static final List<AttributeDefinition> VERTX_ADDRESS_RESOLVER_OPTIONS_ATTRS = new ArrayList<>();
  static {
    VERTX_ADDRESS_RESOLVER_OPTIONS_ATTRS.add(ATTR_HOSTS_PATH);
    VERTX_ADDRESS_RESOLVER_OPTIONS_ATTRS.add(ATTR_HOSTS_VALUE);
    VERTX_ADDRESS_RESOLVER_OPTIONS_ATTRS.add(ATTR_SERVERS);
    VERTX_ADDRESS_RESOLVER_OPTIONS_ATTRS.add(ATTR_OPT_RES_ENABLED);
    VERTX_ADDRESS_RESOLVER_OPTIONS_ATTRS.add(ATTR_CACHE_MIN_TTL);
    VERTX_ADDRESS_RESOLVER_OPTIONS_ATTRS.add(ATTR_MAX_TTL);
    VERTX_ADDRESS_RESOLVER_OPTIONS_ATTRS.add(ATTR_NEGATIVE_TTL);
    VERTX_ADDRESS_RESOLVER_OPTIONS_ATTRS.add(ATTR_QUERY_TIMEOUT);
    VERTX_ADDRESS_RESOLVER_OPTIONS_ATTRS.add(ATTR_MAX_QUERIES);
    VERTX_ADDRESS_RESOLVER_OPTIONS_ATTRS.add(ATTR_RD_FLAG);
    VERTX_ADDRESS_RESOLVER_OPTIONS_ATTRS.add(ATTR_SEARCH_DOMAIN);
    VERTX_ADDRESS_RESOLVER_OPTIONS_ATTRS.add(ATTR_N_DOTS);
    VERTX_ADDRESS_RESOLVER_OPTIONS_ATTRS.add(ATTR_ROTATE_SERVERS);
    VERTX_ADDRESS_RESOLVER_OPTIONS_ATTRS.add(ATTR_ROUND_ROBIN_INET_ADDRESS);
  }

  static List<AttributeDefinition> getVertxAddressResolverOptionsAttrs() {
    return VERTX_ADDRESS_RESOLVER_OPTIONS_ATTRS;
  }

  static AddressResolverResourceDefinition INSTANCE = new AddressResolverResourceDefinition();

  AddressResolverResourceDefinition() {
    super(new SimpleResourceDefinition.Parameters(PathElement.pathElement(ELEMENT_VERTX_OPTION_ADDRESS_RESOLVER),
      VertxSubsystemExtension.getResourceDescriptionResolver(VertxSubsystemExtension.SUBSYSTEM_NAME, ELEMENT_VERTX_OPTION_ADDRESS_RESOLVER))
      .setAddHandler(new AddressResolverResourceDefinition.VertxAddressResolverOptionAddHandler())
      .setRemoveHandler(new RemoveAddressResolverHandler())
      .setCapabilities(VERTX_OPTIONS_ADDRESS_RESOLVER_CAPABILITY)
    );
  }

  @Override
  public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
    super.registerAttributes(resourceRegistration);
    AbstractVertxOptionsResourceDefinition.AttrWriteHandler handler = new AbstractVertxOptionsResourceDefinition.AttrWriteHandler(getVertxAddressResolverOptionsAttrs()) {
      @Override
      protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        return AbstractVertxOptionsResourceDefinition.isAddressResolverUsed(context, context.getCurrentAddressValue());
      }
    };
    for (AttributeDefinition attr : getVertxAddressResolverOptionsAttrs()) {
      resourceRegistration.registerReadWriteAttribute(attr, null, handler);
    }
  }

  private static class RemoveAddressResolverHandler extends AbstractRemoveStepHandler {
    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
      final String name = context.getCurrentAddressValue();
      boolean needsReload = AbstractVertxOptionsResourceDefinition.isAddressResolverUsed(context, name);
      ServiceName serviceName = VERTX_OPTIONS_ADDRESS_RESOLVER_CAPABILITY.getCapabilityServiceName(name);
      context.removeService(serviceName);
      if (needsReload) {
        context.reloadRequired();
      }
    }
  }

  private static class VertxAddressResolverOptionAddHandler extends AbstractAddStepHandler {
    VertxAddressResolverOptionAddHandler() {
      super(new Parameters().addAttribute(VERTX_ADDRESS_RESOLVER_OPTIONS_ATTRS));
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
      final String name = context.getCurrentAddressValue();
      ServiceName addressResolverServiceName = VERTX_OPTIONS_ADDRESS_RESOLVER_CAPABILITY.getCapabilityServiceName(name);
      ServiceBuilder<?> serviceBuilder = context.getServiceTarget().addService(addressResolverServiceName);
      final Consumer<AddressResolverOptions> consumer = serviceBuilder.provides(addressResolverServiceName);
      AddressResolverOptions addressResolverOptions = parseAddressResolverOptions(operation);
      Service addressResolverService = new Service() {

        @Override
        public void start(StartContext startContext) throws StartException {
          consumer.accept(addressResolverOptions);
          VertxOptionsRegistry.getInstance().addAddressResolverOptions(name, addressResolverOptions);
        }

        @Override
        public void stop(StopContext stopContext) {
          VertxOptionsRegistry.getInstance().removeAddressResolverOptions(name);
        }
      };

      serviceBuilder
        .setInstance(addressResolverService)
        .setInitialMode(ServiceController.Mode.ON_DEMAND)
        .install();

    }

    private AddressResolverOptions parseAddressResolverOptions(ModelNode operation) throws OperationFailedException {
      AddressResolverOptions addressResolverOptions = new AddressResolverOptions();
      if (operation.hasDefined(VertxConstants.ATTR_HOSTS_PATH)) {
        addressResolverOptions.setHostsPath(ATTR_HOSTS_PATH.validateOperation(operation).asString());
      }
      if (operation.hasDefined(VertxConstants.ATTR_HOSTS_VALUE)) {
        addressResolverOptions.setHostsValue(Buffer.buffer(ATTR_HOSTS_VALUE.validateOperation(operation).asString()));
      }
      if (operation.hasDefined(VertxConstants.ATTR_SERVERS)) {
        List<ModelNode> list = ATTR_SERVERS.validateOperation(operation).asList();
        addressResolverOptions.setServers(list.stream().map(ModelNode::asString).collect(Collectors.toList()));
      }
      if (operation.hasDefined(VertxConstants.ATTR_OPT_RES_ENABLED)) {
        addressResolverOptions.setOptResourceEnabled(ATTR_OPT_RES_ENABLED.validateOperation(operation).asBoolean());
      }
      if (operation.hasDefined(VertxConstants.ATTR_CACHE_MIN_TTL)) {
        addressResolverOptions.setCacheMinTimeToLive(ATTR_CACHE_MIN_TTL.validateOperation(operation).asInt());
      }
      if (operation.hasDefined(VertxConstants.ATTR_MAX_TTL)) {
        addressResolverOptions.setCacheMaxTimeToLive(ATTR_MAX_TTL.validateOperation(operation).asInt());
      }
      if (operation.hasDefined(VertxConstants.ATTR_NEGATIVE_TTL)) {
        addressResolverOptions.setCacheNegativeTimeToLive(ATTR_NEGATIVE_TTL.validateOperation(operation).asInt());
      }
      if (operation.hasDefined(VertxConstants.ATTR_QUERY_TIMEOUT)) {
        addressResolverOptions.setQueryTimeout(ATTR_QUERY_TIMEOUT.validateOperation(operation).asLong());
      }
      if (operation.hasDefined(VertxConstants.ATTR_MAX_QUERIES)) {
        addressResolverOptions.setMaxQueries(ATTR_MAX_QUERIES.validateOperation(operation).asInt());
      }
      if (operation.hasDefined(VertxConstants.ATTR_RD_FLAG)) {
        addressResolverOptions.setRdFlag(ATTR_RD_FLAG.validateOperation(operation).asBoolean());
      }
      if (operation.hasDefined(VertxConstants.ATTR_SEARCH_DOMAIN)) {
        List<ModelNode> list = ATTR_SEARCH_DOMAIN.validateOperation(operation).asList();
        addressResolverOptions.setSearchDomains(list.stream().map(ModelNode::asString).collect(Collectors.toList()));
      }
      if (operation.hasDefined(VertxConstants.ATTR_N_DOTS)) {
        addressResolverOptions.setNdots(ATTR_N_DOTS.validateOperation(operation).asInt());
      }
      if (operation.hasDefined(VertxConstants.ATTR_ROTATE_SERVERS)) {
        addressResolverOptions.setRotateServers(ATTR_ROTATE_SERVERS.validateOperation(operation).asBoolean());
      }
      if (operation.hasDefined(VertxConstants.ATTR_ROUND_ROBIN_INET_ADDRESS)) {
        addressResolverOptions.setRoundRobinInetAddress(ATTR_ROUND_ROBIN_INET_ADDRESS.validateOperation(operation).asBoolean());
      }
      return addressResolverOptions;
    }

  }

}
