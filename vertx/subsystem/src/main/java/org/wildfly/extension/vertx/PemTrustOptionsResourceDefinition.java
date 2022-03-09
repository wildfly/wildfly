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

import static org.jboss.as.controller.AttributeMarshallers.SIMPLE_ELEMENT;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.AttributeParsers;
import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.PemTrustOptions;

/**
 * This represents a resource at '/subsystem=vertx/pem-trust-option=xx' for a TrustOptions.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
class PemTrustOptionsResourceDefinition extends PersistentResourceDefinition implements VertxConstants {

  // pem-trust-option
  public static final StringListAttributeDefinition ATTR_PEM_KEY_CERT_CERT_PATH = new StringListAttributeDefinition.Builder(VertxConstants.ATTR_PEM_KEY_CERT_CERT_PATH)
    .setRequired(false)
    .setRestartAllServices()
    .setElementValidator(new StringLengthValidator(1))
    .setAllowExpression(true)
    .setAttributeParser(AttributeParser.COMMA_DELIMITED_STRING_LIST)
    .setAttributeMarshaller(AttributeMarshaller.COMMA_STRING_LIST)
    .build();

  public static final SimpleAttributeDefinition ATTR_PEM_VALUE = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_PEM_VALUE, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .setAttributeParser(AttributeParsers.SIMPLE_ELEMENT)
    .setAttributeMarshaller(SIMPLE_ELEMENT)
    .setRestartAllServices()
    .build();

  public static final ObjectTypeAttributeDefinition ATTR_PEM_VALUE_OBJECT = new ObjectTypeAttributeDefinition.Builder(VertxConstants.ATTR_PEM_VALUE, ATTR_PEM_VALUE)
    .setRequired(false)
    .setAllowExpression(true)
    .setRestartAllServices()
    .build();

  public static final ObjectListAttributeDefinition ATTR_PEM_KEY_CERT_CERT_VALUE = new ObjectListAttributeDefinition.Builder(VertxConstants.ATTR_PEM_KEY_CERT_CERT_VALUE, ATTR_PEM_VALUE_OBJECT)
    .setRequired(false)
    .setRestartAllServices()
    .setAllowExpression(true)
    .build();

  private static final List<AttributeDefinition> PEM_TRUST_OPTIONS_ATTRS = new ArrayList<>();
  static {
    PEM_TRUST_OPTIONS_ATTRS.add(ATTR_PEM_KEY_CERT_CERT_PATH);
    PEM_TRUST_OPTIONS_ATTRS.add(ATTR_PEM_KEY_CERT_CERT_VALUE);
  }

  static List<AttributeDefinition> getPemTrustOptionsAttrs() {
    return PEM_TRUST_OPTIONS_ATTRS;
  }

  static PemTrustOptionsResourceDefinition INSTANCE = new PemTrustOptionsResourceDefinition();

  PemTrustOptionsResourceDefinition() {
    super(new SimpleResourceDefinition.Parameters(PathElement.pathElement(ELEMENT_PEM_TRUST),
      VertxSubsystemExtension.getResourceDescriptionResolver(VertxSubsystemExtension.SUBSYSTEM_NAME, ELEMENT_PEM_TRUST))
      .setAddHandler(new AddHandler())
      .setRemoveHandler(new RemoveHandler())
      .setCapabilities(TRUST_OPTIONS_CAPABILITY)
    );
  }

  @Override
  public Collection<AttributeDefinition> getAttributes() {
    return PEM_TRUST_OPTIONS_ATTRS;
  }

  private static class RemoveHandler extends ReloadRequiredRemoveStepHandler {
    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
      final String name = context.getCurrentAddressValue();
      VertxOptionsRegistry.getInstance().removePemTrustOptions(name);
      super.performRuntime(context, operation, model);
    }
  }

  private static class AddHandler extends AbstractAddStepHandler {
    AddHandler() {
      super(new Parameters().addAttribute(PEM_TRUST_OPTIONS_ATTRS));
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
      final String name = context.getCurrentAddressValue();
      PemTrustOptions pemTrustOptions = parseOption(operation, name);
      ServiceBuilder<?> serviceBuilder = context.getServiceTarget().addService(TRUST_OPTIONS_CAPABILITY.getCapabilityServiceName(name));
      final Supplier<ServerEnvironment> serverEnvSupplier = serviceBuilder.requires(ServerEnvironmentService.SERVICE_NAME);
      VertxOptionValueService<PemTrustOptions> optionValueService = new VertxOptionValueService<PemTrustOptions>(pemTrustOptions) {

        @Override
        public void start(StartContext startContext) throws StartException {
          List<String> certPaths = pemTrustOptions.getCertPaths();
          if (certPaths != null && certPaths.size() > 0) {
            List<String> canonicalCertPaths = certPaths.stream().map(path -> {
              if (path != null && path.trim().length() > 0) {
                if (!path.startsWith("/")) {
                  return new File(serverEnvSupplier.get().getServerConfigurationDir(), path).getAbsolutePath();
                }
              }
              return path;
            }).collect(Collectors.toList());
            certPaths.clear();
            certPaths.addAll(canonicalCertPaths);
          }
          VertxOptionsRegistry.getInstance().addPemTrustOptions(name, pemTrustOptions);
        }

        @Override
        public void stop(StopContext stopContext) {
          VertxOptionsRegistry.getInstance().removePemTrustOptions(name);
        }
      };
      serviceBuilder
        .setInstance(optionValueService)
        .setInitialMode(ServiceController.Mode.LAZY)
        .install();
    }

    private PemTrustOptions parseOption(ModelNode operation, String name) throws OperationFailedException {
      final PemTrustOptions pemTrustOptions = new PemTrustOptions();
      if (operation.hasDefined(ATTR_PEM_KEY_CERT_CERT_PATH.getName())) {
        ATTR_PEM_KEY_CERT_CERT_PATH.validateOperation(operation).asList().forEach(m -> pemTrustOptions.addCertPath(m.asString()));
      }
      if (operation.hasDefined(ATTR_PEM_KEY_CERT_CERT_VALUE.getName())) {
        ATTR_PEM_KEY_CERT_CERT_VALUE.validateOperation(operation).asList().forEach(m -> pemTrustOptions.addCertValue(Buffer.buffer(m.asString())));
      }
      return pemTrustOptions;
    }

  }

}
