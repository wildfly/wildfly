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

import static org.wildfly.extension.vertx.VertxConstants.*;
import static org.wildfly.extension.vertx.logging.VertxLogger.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.function.Supplier;

import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
class VertxOptionFileResourceDefinition extends AbstractVertxOptionsResourceDefinition {

  static VertxOptionFileResourceDefinition INSTANCE = new VertxOptionFileResourceDefinition();

  VertxOptionFileResourceDefinition() {
    super(new SimpleResourceDefinition.Parameters(PathElement.pathElement(ELEMENT_VERTX_OPTIONS_FILE),
      VertxSubsystemExtension.getResourceDescriptionResolver(VertxSubsystemExtension.SUBSYSTEM_NAME, ELEMENT_VERTX_OPTIONS_FILE))
      .setAddHandler(new VertxOptionFileAddHandler())
      .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
      .setCapabilities(VERTX_OPTIONS_CAPABILITY)
    );
  }

  @Override
  public Collection<AttributeDefinition> getAttributes() {
    return VertxOptionsAttributes.getVertxOptionsFileAttributes();
  }

  static class VertxOptionFileAddHandler extends AbstractAddStepHandler {

    VertxOptionFileAddHandler() {
        super(new Parameters().addAttribute(VertxOptionsAttributes.getVertxOptionsFileAttributes()));
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
      final String name = context.getCurrentAddressValue();
      if (DEFAULT_VERTX_OPTION_NAME.equals(name)) {
        throw VERTX_LOGGER.optionNameIsReserved(name);
      }

      final String optionFilePath = operation.hasDefined(ATTR_PATH) ? VertxOptionsAttributes.VERTX_OPTION_FILE_PATH.resolveModelAttribute(context, operation).asString() : null;
      if (optionFilePath == null || optionFilePath.trim().equals("")) {
        throw VERTX_LOGGER.noOptionsFileSpecified(name);
      }
      ServiceName vertxServiceName = VertxOptionFileResourceDefinition.VERTX_OPTIONS_CAPABILITY.getCapabilityServiceName(name);
      ServiceBuilder<?> vertxServiceBuilder = context.getServiceTarget().addService(vertxServiceName);
      Supplier<ServerEnvironment> serverEnvSupplier = vertxServiceBuilder.requires(ServerEnvironmentService.SERVICE_NAME);
      VertxOptions vertxOptions = new VertxOptions(readJsonFromFile(optionFilePath, serverEnvSupplier.get().getServerConfigurationDir()));
      NamedVertxOptions namedVertxOptions = new NamedVertxOptions(name, vertxOptions);
      vertxServiceBuilder.setInstance(new NamedVertxOptionsService(namedVertxOptions));
      vertxServiceBuilder
        .setInitialMode(ServiceController.Mode.ACTIVE)
        .install();
    }

  }

  static JsonObject readJsonFromFile(String vertxOptionsFile, File configDir) throws OperationFailedException {
    Path path = vertxOptionsFile.startsWith("/") ? Paths.get(vertxOptionsFile) : Paths.get(configDir.getPath(), vertxOptionsFile);
    if (Files.exists(path) && Files.isReadable(path)) {
      String jsonContent;
      try (InputStream inputStream = Files.newInputStream(path);
           ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        StreamUtils.copyStream(inputStream, outputStream);
        jsonContent = outputStream.toString();
      } catch (IOException e) {
        throw VERTX_LOGGER.failedToReadVertxOptions(vertxOptionsFile, e);
      }
      if (jsonContent != null) {
        return new JsonObject(jsonContent);
      }
      return new JsonObject();
    } else {
      throw VERTX_LOGGER.cannotReadVertxOptionsFile(vertxOptionsFile);
    }
  }

}
