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

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.controller.AttributeMarshallers.SIMPLE_ELEMENT;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.AttributeParsers;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.parsing.ParseUtils;
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
import org.jboss.staxmapper.XMLExtendedStreamReader;

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

  static final AttributeParser PEM_VALUE_PARSER = new AttributeParser() {

    @Override
    public boolean isParseAsElement() {
      return true;
    }

    @Override
    public void parseElement(AttributeDefinition attribute, XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
      SimpleListAttributeDefinition attr = (SimpleListAttributeDefinition) attribute;
      AttributeDefinition valueType = attr.getValueType();
      ModelNode listValue = new ModelNode();
      listValue.setEmptyList();
      while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
        if (valueType.getXmlName().equals(reader.getLocalName())) {
          ModelNode op = listValue.add();
          valueType.getParser().parseElement(valueType, reader, op);
        } else {
          throw ParseUtils.unexpectedElement(reader, Collections.singleton(valueType.getXmlName()));
        }
        if (!reader.isEndElement()) {
          ParseUtils.requireNoContent(reader);
        }
      }
      operation.get(attribute.getName()).set(listValue.asList().stream().map(m -> m.get(valueType.getXmlName())).collect(Collectors.toList()));
    }
  };

  static final AttributeMarshaller PEM_VALUE_MARSHALLER = new AttributeMarshaller() {
    @Override
    public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
      SimpleListAttributeDefinition attr = (SimpleListAttributeDefinition) attribute;
      AttributeDefinition valueType = attr.getValueType();
      if (resourceModel.hasDefined(attribute.getName())) {
        writer.writeStartElement(attribute.getXmlName());
        for (ModelNode res : resourceModel.get(attribute.getName()).asList()) {
          ModelNode subResource = new ModelNode();
          subResource.setEmptyObject();
          subResource.get(valueType.getName()).set(res);
          valueType.getMarshaller().marshallAsElement(valueType, subResource, true, writer);
        }
        writer.writeEndElement();
      }
    }

    @Override
    public boolean isMarshallableAsElement() {
      return true;
    }
  };

  public static final SimpleListAttributeDefinition ATTR_PEM_KEY_CERT_CERT_VALUE = new SimpleListAttributeDefinition.Builder(VertxConstants.ATTR_PEM_KEY_CERT_CERT_VALUE, ATTR_PEM_VALUE)
    .setRequired(false)
    .setRestartAllServices()
    .setAllowExpression(true)
    .setAttributeParser(PEM_VALUE_PARSER)
    .setAttributeMarshaller(PEM_VALUE_MARSHALLER)
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
      final ServiceName serviceName = TRUST_OPTIONS_CAPABILITY.getCapabilityServiceName(name);
      ServiceBuilder<?> serviceBuilder = context.getServiceTarget().addService(serviceName);
      final Consumer<PemTrustOptions> consumer = serviceBuilder.provides(serviceName);
      final Supplier<ServerEnvironment> serverEnvSupplier = serviceBuilder.requires(ServerEnvironmentService.SERVICE_NAME);
      Service optionValueService = new Service() {

        @Override
        public void start(StartContext startContext) throws StartException {
          List<String> certPaths = pemTrustOptions.getCertPaths();
          if (certPaths != null && certPaths.size() > 0) {
            List<String> canonicalCertPaths = certPaths.stream().map(path -> {
              if (path != null && path.trim().length() > 0) {
                return Paths.get(path).isAbsolute() ? path : Paths.get(serverEnvSupplier.get().getServerConfigurationDir().getPath(), path).toString();
              }
              return path;
            }).collect(Collectors.toList());
            certPaths.clear();
            certPaths.addAll(canonicalCertPaths);
          }
          consumer.accept(pemTrustOptions);
          VertxOptionsRegistry.getInstance().addPemTrustOptions(name, pemTrustOptions);
        }

        @Override
        public void stop(StopContext stopContext) {
          VertxOptionsRegistry.getInstance().removePemTrustOptions(name);
        }
      };
      serviceBuilder
        .setInstance(optionValueService)
        .setInitialMode(ServiceController.Mode.ON_DEMAND)
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
