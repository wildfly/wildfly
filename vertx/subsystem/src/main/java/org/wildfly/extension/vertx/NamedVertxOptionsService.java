/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.vertx;

import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.wildfly.extension.vertx.VertxConstants.ATTR_FS_FILE_CACHE_DIR;
import static org.wildfly.extension.vertx.VertxConstants.ELEMENT_VERTX_OPTION_ADDRESS_RESOLVER;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class NamedVertxOptionsService implements Service {

  private final NamedVertxOptions namedVertxOptions;
  private final Consumer<NamedVertxOptions> consumer;
  private final Supplier<AddressResolverOptions> addressResolverOptionsSupplier;
  private final Supplier<ServerEnvironment> serverEnvironmentSupplier;
  private final boolean defaultFileCacheDir;

  NamedVertxOptionsService(NamedVertxOptions namedVertxOptions,
                           Consumer<NamedVertxOptions> consumer) {
    this(namedVertxOptions, null, null, false, consumer);
  }

  NamedVertxOptionsService(NamedVertxOptions namedVertxOptions,
                           Supplier<AddressResolverOptions> addressResolverOptionsSupplier,
                           Supplier<ServerEnvironment> serverEnvironmentSupplier,
                           boolean defaultFileCacheDir,
                           Consumer<NamedVertxOptions> consumer) {
    this.namedVertxOptions = namedVertxOptions;
    this.addressResolverOptionsSupplier = addressResolverOptionsSupplier;
    this.serverEnvironmentSupplier = serverEnvironmentSupplier;
    this.defaultFileCacheDir = defaultFileCacheDir;
    this.consumer = consumer;
  }

  /**
   * Install NamedVertxOptionsService from '/subsystem=vertx/vertx-option=xx:add()'
   * <p>
   *     When 'file-cache-dir' is not configured, it defaults to '${jboss.server.temp.dir}/vertx-cache'
   * </p>
   * @param context the OperationContext to add a VertxOptions with a name
   * @param operation the operation ModelNode to add a VertxOptions with a name
   * @throws OperationFailedException when anything goes wrong
   */
  static void installVertxOptionsService(OperationContext context, ModelNode operation) throws OperationFailedException {
    final String name = context.getCurrentAddressValue();
    VertxOptions vertxOptions = VertxOptionsResourceDefinition.parseOptions(operation);
    ServiceName vertxServiceName = VertxOptionFileResourceDefinition.VERTX_OPTIONS_CAPABILITY.getCapabilityServiceName(name);
    ServiceBuilder<?> vertxServiceBuilder = context.getCapabilityServiceTarget().addService();
    Supplier<AddressResolverOptions> addressResolverOptionsSupplier = null;
    if (operation.hasDefined(ELEMENT_VERTX_OPTION_ADDRESS_RESOLVER)) {
      String addressResolverOptionName = VertxOptionsAttributes.ATTR_VERTX_OPTION_ADDRESS_RESOLVER.validateOperation(operation).asString();
      ServiceName addressResolverServiceName = AddressResolverResourceDefinition.VERTX_OPTIONS_ADDRESS_RESOLVER_CAPABILITY.getCapabilityServiceName(addressResolverOptionName);
      addressResolverOptionsSupplier = vertxServiceBuilder.requires(addressResolverServiceName);
    }
    NamedVertxOptions namedVertxOptions = new NamedVertxOptions(name, vertxOptions);
    ServiceName serverEnvServiceName = context.getCapabilityServiceName(ServerEnvironment.SERVICE_DESCRIPTOR);
    Supplier<ServerEnvironment> environmentSupplier = vertxServiceBuilder.requires(serverEnvServiceName);
    vertxServiceBuilder.setInstance(new NamedVertxOptionsService(namedVertxOptions, addressResolverOptionsSupplier,
            environmentSupplier, !operation.hasDefined(ATTR_FS_FILE_CACHE_DIR), vertxServiceBuilder.provides(vertxServiceName)));
    vertxServiceBuilder
            .setInitialMode(ServiceController.Mode.ACTIVE)
            .install();
  }

  @Override
  public void start(StartContext startContext) throws StartException {
    if (this.addressResolverOptionsSupplier != null && this.addressResolverOptionsSupplier.get() != null) {
      this.namedVertxOptions.getVertxOptions().setAddressResolverOptions(this.addressResolverOptionsSupplier.get());
    }
    if (defaultFileCacheDir) {
      String defaultCacheDir = serverEnvironmentSupplier.get().getServerTempDir().toPath().resolve(Path.of("vertx-cache")).normalize().toString();
      this.namedVertxOptions.getVertxOptions().getFileSystemOptions().setFileCacheDir(defaultCacheDir);
    }
    this.consumer.accept(this.namedVertxOptions);
    VertxOptionsRegistry.getInstance().addVertxOptions(this.namedVertxOptions);
  }

  @Override
  public void stop(StopContext stopContext) {
    VertxOptionsRegistry.getInstance().removeVertxOptions(this.namedVertxOptions.getName());
  }

}
