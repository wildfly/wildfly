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

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.PemKeyCertOptions;

/**
 * This represents a resource at '/subsystem=vertx/pem-key-cert-option=xx' for a KeyCertOptions.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
class PemKeyCertOptionsResourceDefinition extends PersistentResourceDefinition implements VertxConstants {

  // pem-key-cert-option
  public static final StringListAttributeDefinition ATTR_PEM_KEY_CERT_KEY_PATH = new StringListAttributeDefinition.Builder(VertxConstants.ATTR_PEM_KEY_CERT_KEY_PATH)
    .setRequired(false)
    .setRestartAllServices()
    .setElementValidator(new StringLengthValidator(1))
    .setAllowExpression(true)
    .setAttributeParser(AttributeParser.COMMA_DELIMITED_STRING_LIST)
    .setAttributeMarshaller(AttributeMarshaller.COMMA_STRING_LIST)
    .build();

  public static final SimpleListAttributeDefinition ATTR_PEM_KEY_CERT_KEY_VALUE = new SimpleListAttributeDefinition.Builder(VertxConstants.ATTR_PEM_KEY_CERT_KEY_VALUE, PemTrustOptionsResourceDefinition.ATTR_PEM_VALUE)
    .setRequired(false)
    .setRestartAllServices()
    .setAttributeParser(PEM_VALUE_PARSER)
    .setAttributeMarshaller(PEM_VALUE_MARSHALLER)
    .setAllowExpression(true)
    .build();

  public static final StringListAttributeDefinition ATTR_PEM_KEY_CERT_CERT_PATH = new StringListAttributeDefinition.Builder(VertxConstants.ATTR_PEM_KEY_CERT_CERT_PATH)
    .setRequired(false)
    .setRestartAllServices()
    .setElementValidator(new StringLengthValidator(1))
    .setAllowExpression(true)
    .setAttributeParser(AttributeParser.COMMA_DELIMITED_STRING_LIST)
    .setAttributeMarshaller(AttributeMarshaller.COMMA_STRING_LIST)
    .build();

  public static final SimpleListAttributeDefinition ATTR_PEM_KEY_CERT_CERT_VALUE = new SimpleListAttributeDefinition.Builder(VertxConstants.ATTR_PEM_KEY_CERT_CERT_VALUE, PemTrustOptionsResourceDefinition.ATTR_PEM_VALUE)
    .setRequired(false)
    .setRestartAllServices()
    .setAttributeParser(PEM_VALUE_PARSER)
    .setAttributeMarshaller(PEM_VALUE_MARSHALLER)
    .setAllowExpression(true)
    .build();

  private static final List<AttributeDefinition> PEM_KEY_CERT_OPTIONS_ATTRS = new ArrayList<>();
  static {
    PEM_KEY_CERT_OPTIONS_ATTRS.add(ATTR_PEM_KEY_CERT_KEY_PATH);
    PEM_KEY_CERT_OPTIONS_ATTRS.add(ATTR_PEM_KEY_CERT_KEY_VALUE);
    PEM_KEY_CERT_OPTIONS_ATTRS.add(ATTR_PEM_KEY_CERT_CERT_PATH);
    PEM_KEY_CERT_OPTIONS_ATTRS.add(ATTR_PEM_KEY_CERT_CERT_VALUE);
  }

  static List<AttributeDefinition> getPemKeyCertOptionsAttrs() {
    return PEM_KEY_CERT_OPTIONS_ATTRS;
  }

  static PemKeyCertOptionsResourceDefinition INSTANCE = new PemKeyCertOptionsResourceDefinition();

  PemKeyCertOptionsResourceDefinition() {
    super(new SimpleResourceDefinition.Parameters(PathElement.pathElement(ELEMENT_PEM_KEY_CERT),
      VertxSubsystemExtension.getResourceDescriptionResolver(VertxSubsystemExtension.SUBSYSTEM_NAME, ELEMENT_PEM_KEY_CERT))
      .setAddHandler(new AddHandler())
      .setRemoveHandler(new RemoveHandler())
      .setCapabilities(KEY_CERT_OPTIONS_CAPABILITY)
    );
  }

  @Override
  public Collection<AttributeDefinition> getAttributes() {
    return PEM_KEY_CERT_OPTIONS_ATTRS;
  }

  private static class RemoveHandler extends ReloadRequiredRemoveStepHandler {
    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
      final String name = context.getCurrentAddressValue();
      VertxOptionsRegistry.getInstance().removePemKeyCertOptions(name);
      super.performRuntime(context, operation, model);
    }
  }

  private static class AddHandler extends AbstractAddStepHandler {
    AddHandler() {
      super(new Parameters().addAttribute(PEM_KEY_CERT_OPTIONS_ATTRS));
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
      final String name = context.getCurrentAddressValue();
      PemKeyCertOptions pemKeyCertOptions = parseOption(operation, name);
      final ServiceName serviceName = KEY_CERT_OPTIONS_CAPABILITY.getCapabilityServiceName(name);
      ServiceBuilder<?> serviceBuilder = context.getServiceTarget().addService(serviceName);
      final Supplier<ServerEnvironment> serverEnvSupplier = serviceBuilder.requires(ServerEnvironmentService.SERVICE_NAME);
      final Consumer<PemKeyCertOptions> consumer = serviceBuilder.provides(serviceName);
      Service optionValueService = new Service() {

        @Override
        public void start(StartContext startContext) throws StartException {
          List<String> keyPaths = pemKeyCertOptions.getKeyPaths();
          if (keyPaths != null && keyPaths.size() > 0) {
            List<String> canonicalKeyPaths = keyPaths.stream().map(path -> {
              if (path != null && path.trim().length() > 0) {
                return Paths.get(path).isAbsolute() ? path : Paths.get(serverEnvSupplier.get().getServerConfigurationDir().getPath(), path).toString();
              }
              return path;
            }).collect(Collectors.toList());
            pemKeyCertOptions.setKeyPaths(canonicalKeyPaths);
          }
          List<String> certPaths = pemKeyCertOptions.getCertPaths();
          if (certPaths != null && certPaths.size() > 0) {
            List<String> canonicalCertPaths = certPaths.stream().map(path -> {
              if (path != null && path.trim().length() > 0) {
                return Paths.get(path).isAbsolute() ? path : Paths.get(serverEnvSupplier.get().getServerConfigurationDir().getPath(), path).toString();
              }
              return path;
            }).collect(Collectors.toList());
            pemKeyCertOptions.setCertPaths(canonicalCertPaths);
          }
          consumer.accept(pemKeyCertOptions);
          VertxOptionsRegistry.getInstance().addPemKeyCertOptions(name, pemKeyCertOptions);
        }

        @Override
        public void stop(StopContext stopContext) {
          VertxOptionsRegistry.getInstance().removePemKeyCertOptions(name);
        }
      };
      serviceBuilder
        .setInstance(optionValueService)
        .setInitialMode(ServiceController.Mode.ON_DEMAND)
        .install();
    }

    private PemKeyCertOptions parseOption(ModelNode operation, String name) throws OperationFailedException {
      final PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();
      if (operation.hasDefined(ATTR_PEM_KEY_CERT_KEY_PATH.getName())) {
        pemKeyCertOptions.setKeyPaths(ATTR_PEM_KEY_CERT_KEY_PATH.validateOperation(operation).asList().stream()
          .map(ModelNode::asString).collect(Collectors.toList()));
      }
      if (operation.hasDefined(ATTR_PEM_KEY_CERT_KEY_VALUE.getName())) {
        pemKeyCertOptions.setKeyValues(ATTR_PEM_KEY_CERT_KEY_VALUE.validateOperation(operation).asList().stream()
          .map(mn -> Buffer.buffer(mn.asString())).collect(Collectors.toList()));
      }
      if (operation.hasDefined(ATTR_PEM_KEY_CERT_CERT_PATH.getName())) {
        pemKeyCertOptions.setCertPaths(ATTR_PEM_KEY_CERT_CERT_PATH.validateOperation(operation).asList().stream()
          .map(ModelNode::asString).collect(Collectors.toList()));
      }
      if (operation.hasDefined(ATTR_PEM_KEY_CERT_CERT_VALUE.getName())) {
        pemKeyCertOptions.setCertValues(ATTR_PEM_KEY_CERT_CERT_VALUE.validateOperation(operation).asList().stream()
          .map(mn -> Buffer.buffer(mn.asString())).collect(Collectors.toList()));
      }
      return pemKeyCertOptions;
    }

  }

}
