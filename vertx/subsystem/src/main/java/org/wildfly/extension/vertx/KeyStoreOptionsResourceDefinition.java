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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.vertx.logging.VertxLogger;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.KeyStoreOptions;
import io.vertx.core.net.KeyStoreOptionsBase;
import io.vertx.core.net.PfxOptions;
import io.vertx.core.net.TrustOptions;

/**
 * This represents a resource at '/subsystem=vertx/key-store-option=xx', which can be used as either KeyCertOptions or TrustOptions.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
class KeyStoreOptionsResourceDefinition extends PersistentResourceDefinition implements VertxConstants {

  // key-store-option
  public static final SimpleAttributeDefinition ATTR_KEYSTORE_PROVIDER = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_KEYSTORE_PROVIDER, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .setRestartAllServices()
    .build();

  public static final SimpleAttributeDefinition ATTR_KEYSTORE_TYPE = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_KEYSTORE_TYPE, ModelType.STRING)
    .setRequired(true)
    .setAllowExpression(true)
    .setRestartAllServices()
    .build();

  public static final SimpleAttributeDefinition ATTR_KEYSTORE_PASSWORD = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_KEYSTORE_PASSWORD, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .setRestartAllServices()
    .build();

  public static final SimpleAttributeDefinition ATTR_KEYSTORE_PATH = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_KEYSTORE_PATH, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .setRestartAllServices()
    .build();

  public static final SimpleAttributeDefinition ATTR_KEYSTORE_VALUE = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_KEYSTORE_VALUE, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .setRestartAllServices()
    .build();

  public static final SimpleAttributeDefinition ATTR_KEYSTORE_ALIAS = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_KEYSTORE_ALIAS, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .setRestartAllServices()
    .build();

  public static final SimpleAttributeDefinition ATTR_KEYSTORE_ALIAS_PASSWORD = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_KEYSTORE_ALIAS_PASSWORD, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .setRestartAllServices()
    .build();

  private static final List<AttributeDefinition> KEYSTORE_OPTIONS_ATTRS = new ArrayList<>();
  static {
    KEYSTORE_OPTIONS_ATTRS.add(ATTR_KEYSTORE_PROVIDER);
    KEYSTORE_OPTIONS_ATTRS.add(ATTR_KEYSTORE_TYPE);
    KEYSTORE_OPTIONS_ATTRS.add(ATTR_KEYSTORE_PASSWORD);
    KEYSTORE_OPTIONS_ATTRS.add(ATTR_KEYSTORE_PATH);
    KEYSTORE_OPTIONS_ATTRS.add(ATTR_KEYSTORE_VALUE);
    KEYSTORE_OPTIONS_ATTRS.add(ATTR_KEYSTORE_ALIAS);
    KEYSTORE_OPTIONS_ATTRS.add(ATTR_KEYSTORE_ALIAS_PASSWORD);
  }

  static List<AttributeDefinition> getKeyStoreOptionsAttrs() {
    return KEYSTORE_OPTIONS_ATTRS;
  }

  static KeyStoreOptionsResourceDefinition INSTANCE = new KeyStoreOptionsResourceDefinition();

  KeyStoreOptionsResourceDefinition() {
    super(new SimpleResourceDefinition.Parameters(PathElement.pathElement(ELEMENT_KEY_STORE),
      VertxSubsystemExtension.getResourceDescriptionResolver(VertxSubsystemExtension.SUBSYSTEM_NAME, ELEMENT_KEY_STORE))
      .setAddHandler(new AddHandler())
      .setRemoveHandler(new RemoveHandler())
      .setCapabilities(KEY_CERT_OPTIONS_CAPABILITY, TRUST_OPTIONS_CAPABILITY)
    );
  }

  @Override
  public Collection<AttributeDefinition> getAttributes() {
    return KEYSTORE_OPTIONS_ATTRS;
  }

  private static class RemoveHandler extends ReloadRequiredRemoveStepHandler {
    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
      super.performRuntime(context, operation, model);
    }
  }

  private static class AddHandler extends AbstractAddStepHandler {
    AddHandler() {
      super(new Parameters().addAttribute(KEYSTORE_OPTIONS_ATTRS));
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
      final String name = context.getCurrentAddressValue();
      KeyStoreOptionsBase keyStoreOptions = parseOption(operation, name);
      final ServiceName serviceName = KEY_CERT_OPTIONS_CAPABILITY.getCapabilityServiceName(name);
      ServiceBuilder<?> serviceBuilder = context.getServiceTarget().addService(serviceName);
      final Consumer<TrustOptions> trustOptionsConsumer = serviceBuilder.provides(serviceName);
      final Consumer<KeyCertOptions> keyCertOptionsConsumer = serviceBuilder.provides(TRUST_OPTIONS_CAPABILITY.getCapabilityServiceName(name));
      final Supplier<ServerEnvironment> serverEnvSupplier = serviceBuilder.requires(ServerEnvironmentService.SERVICE_NAME);
      Service optionValueService = new Service() {

        @Override
        public void start(StartContext startContext) throws StartException {
          String path = keyStoreOptions.getPath();
          if (path != null && path.trim().length() > 0) {
            if (!path.startsWith("/")) {
              keyStoreOptions.setPath(new File(serverEnvSupplier.get().getServerConfigurationDir(), path).getAbsolutePath());
            }
          }
          VertxOptionsRegistry.getInstance().addKeyStoreOptions(name, keyStoreOptions);
          trustOptionsConsumer.accept(keyStoreOptions);
          keyCertOptionsConsumer.accept(keyStoreOptions);
        }

        @Override
        public void stop(StopContext stopContext) {
          VertxOptionsRegistry.getInstance().removeKeyStoreOptions(name);
        }
      };
      serviceBuilder
        .setInstance(optionValueService)
        .setInitialMode(ServiceController.Mode.LAZY)
        .install();
    }

    private KeyStoreOptionsBase parseOption(ModelNode operation, String name) throws OperationFailedException {
      if (!operation.hasDefined(ATTR_KEYSTORE_PATH.getName()) && !operation.hasDefined(ATTR_KEYSTORE_VALUE.getName())) {
        throw VertxLogger.VERTX_LOGGER.atLeastPathOrValueDefined(name);
      }
      final KeyStoreOptionsBase keyStoreOptions;
      String type = ATTR_KEYSTORE_TYPE.validateOperation(operation).asString().toLowerCase(Locale.ENGLISH);
      if (type.equals(VertxConstants.KEY_STORE_TYPE_JKS)) {
        keyStoreOptions = new JksOptions();
      } else if (type.equals(VertxConstants.KEY_STORE_TYPE_PKCS12)) {
        keyStoreOptions = new PfxOptions();
      } else {
        keyStoreOptions = new KeyStoreOptions();
        ((KeyStoreOptions)keyStoreOptions).setType(type);
        if (operation.hasDefined(ATTR_KEYSTORE_PROVIDER.getName())) {
          ((KeyStoreOptions)keyStoreOptions).setProvider(ATTR_KEYSTORE_PROVIDER.validateOperation(operation).asString());
        }
      }
      if (operation.hasDefined(ATTR_KEYSTORE_ALIAS.getName())) {
        keyStoreOptions.setAlias(ATTR_KEYSTORE_ALIAS.validateOperation(operation).asString());
      }
      if (operation.hasDefined(ATTR_KEYSTORE_ALIAS_PASSWORD.getName())) {
        keyStoreOptions.setAliasPassword(ATTR_KEYSTORE_ALIAS_PASSWORD.validateOperation(operation).asString());
      }
      if (operation.hasDefined(ATTR_KEYSTORE_PASSWORD.getName())) {
        keyStoreOptions.setPassword(ATTR_KEYSTORE_PASSWORD.validateOperation(operation).asString());
      }
      if (operation.hasDefined(ATTR_KEYSTORE_PATH.getName())) {
        keyStoreOptions.setPath(ATTR_KEYSTORE_PATH.validateOperation(operation).asString());
      }
      if (operation.hasDefined(ATTR_KEYSTORE_VALUE.getName())) {
        keyStoreOptions.setValue(Buffer.buffer(ATTR_KEYSTORE_VALUE.validateOperation(operation).asString()));
      }
      return keyStoreOptions;
    }

  }

}
