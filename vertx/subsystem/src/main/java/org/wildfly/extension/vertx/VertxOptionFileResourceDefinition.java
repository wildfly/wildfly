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

import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.wildfly.extension.vertx.VertxConstants.ATTR_PATH;
import static org.wildfly.extension.vertx.VertxConstants.ELEMENT_VERTX_OPTIONS_FILE;
import static org.wildfly.extension.vertx.logging.VertxLogger.VERTX_LOGGER;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
class VertxOptionFileResourceDefinition extends AbstractVertxOptionsResourceDefinition {

  static VertxOptionFileResourceDefinition INSTANCE = new VertxOptionFileResourceDefinition();

  VertxOptionFileResourceDefinition() {
    super(new SimpleResourceDefinition.Parameters(PathElement.pathElement(ELEMENT_VERTX_OPTIONS_FILE),
      VertxSubsystemExtension.getResourceDescriptionResolver(VertxSubsystemExtension.SUBSYSTEM_NAME, ELEMENT_VERTX_OPTIONS_FILE))
      .setAddHandler(new VertxOptionFileAddHandler())
      .setRemoveHandler(new VertxOptionRemoveHandler())
      .setCapabilities(VERTX_OPTIONS_CAPABILITY)
    );
  }

  @Override
  public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
    super.registerAttributes(resourceRegistration);
    AttrWriteHandler handler = new AttrWriteHandler();
    for (AttributeDefinition attr : VertxOptionsAttributes.getVertxOptionsFileAttributes()) {
      resourceRegistration.registerReadWriteAttribute(attr, null, handler);
    }
  }

  static class VertxOptionFileAddHandler extends AbstractAddStepHandler {

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
      final String name = context.getCurrentAddressValue();
      final String optionFilePath = operation.hasDefined(ATTR_PATH) ? VertxOptionsAttributes.VERTX_OPTION_FILE_PATH.resolveModelAttribute(context, operation).asString() : null;
      if (optionFilePath == null || optionFilePath.trim().isEmpty()) {
        throw VERTX_LOGGER.noOptionsFileSpecified(name);
      }
      ServiceName vertxServiceName = VertxOptionFileResourceDefinition.VERTX_OPTIONS_CAPABILITY.getCapabilityServiceName(name);
      ServiceBuilder<?> vertxServiceBuilder = context.getCapabilityServiceTarget().addService();
      Consumer<NamedVertxOptions> consumer = vertxServiceBuilder.provides(vertxServiceName);
      Supplier<ServerEnvironment> serverEnvSupplier = vertxServiceBuilder.requires(ServerEnvironmentService.SERVICE_NAME);
      VertxOptions vertxOptions = new VertxOptions(readJsonFromFile(optionFilePath, serverEnvSupplier.get().getServerConfigurationDir()));
      NamedVertxOptions namedVertxOptions = new NamedVertxOptions(name, vertxOptions);
      vertxServiceBuilder.setInstance(new NamedVertxOptionsService(namedVertxOptions, consumer));
      vertxServiceBuilder
        .setInitialMode(ServiceController.Mode.ACTIVE)
        .install();
    }

  }

  static JsonObject readJsonFromFile(String vertxOptionsFile, File configDir) throws OperationFailedException {
    Path path = Paths.get(vertxOptionsFile);
    if (path.isAbsolute()) {
      throw VERTX_LOGGER.absoluteDirectoryNotAllowed(vertxOptionsFile);
    }
    path = configDir.toPath().resolve(vertxOptionsFile);
    if (Files.exists(path) && Files.isReadable(path)) {
      String jsonContent;
      try {
        jsonContent = Files.readString(path);
      } catch (IOException e) {
        throw VERTX_LOGGER.failedToReadVertxOptions(path.toString(), e);
      }
      if (jsonContent != null) {
        return new JsonObject(jsonContent);
      }
      return new JsonObject();
    } else {
      throw VERTX_LOGGER.cannotReadVertxOptionsFile(path.toString());
    }
  }

}
