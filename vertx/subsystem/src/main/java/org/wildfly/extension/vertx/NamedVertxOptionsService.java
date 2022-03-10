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
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.tracing.TracingOptions;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class NamedVertxOptionsService implements Service {

  private final NamedVertxOptions namedVertxOptions;
  private final Consumer<NamedVertxOptions> consumer;
  private final Supplier<ServerEnvironment> serverEnvSupplier;
  private final Supplier<AddressResolverOptions> addressResolverOptionsSupplier;
  private final Supplier<EventBusOptions> eventBusOptionsSupplier;
  private final Supplier<MetricsOptions> metricsOptionsSupplier;
  private final Supplier<TracingOptions> tracingOptionsSupplier;

  NamedVertxOptionsService(NamedVertxOptions namedVertxOptions, Supplier<ServerEnvironment> serverEnvSupplier, Consumer<NamedVertxOptions> consumer) {
    this(namedVertxOptions, serverEnvSupplier, null, null, null, null, consumer);
  }

  NamedVertxOptionsService(NamedVertxOptions namedVertxOptions, Supplier<ServerEnvironment> serverEnvSupplier,
                           Supplier<AddressResolverOptions> addressResolverOptionsSupplier,
                           Supplier<EventBusOptions> eventBusOptionsSupplier,
                           Supplier<MetricsOptions> metricsOptionsSupplier,
                           Supplier<TracingOptions> tracingOptionsSupplier,
                           Consumer<NamedVertxOptions> consumer) {
    this.namedVertxOptions = namedVertxOptions;
    this.serverEnvSupplier = serverEnvSupplier;
    this.addressResolverOptionsSupplier = addressResolverOptionsSupplier;
    this.eventBusOptionsSupplier = eventBusOptionsSupplier;
    this.metricsOptionsSupplier = metricsOptionsSupplier;
    this.tracingOptionsSupplier = tracingOptionsSupplier;
    this.consumer = consumer;
  }

  static void installService(OperationContext context, NamedVertxOptions namedVertxOptions,
                             String addressResolverOptionName, String eventBusOptionName,
                             String metricsOptionName, String tracingOptionName) {
    ServiceName vertxServiceName = VertxOptionFileResourceDefinition.VERTX_OPTIONS_CAPABILITY.getCapabilityServiceName(namedVertxOptions.getName());
    ServiceBuilder<?> vertxServiceBuilder = context.getServiceTarget().addService(vertxServiceName);
    Consumer<NamedVertxOptions> consumer = vertxServiceBuilder.provides(vertxServiceName);
    Supplier<ServerEnvironment> serverEnvSupplier = vertxServiceBuilder.requires(ServerEnvironmentService.SERVICE_NAME);
    Supplier<AddressResolverOptions> addressResolverOptionsSupplier = null;
    if (addressResolverOptionName != null) {
      addressResolverOptionsSupplier = vertxServiceBuilder.requires(AddressResolverResourceDefinition.VERTX_OPTIONS_ADDRESS_RESOLVER_CAPABILITY.getCapabilityServiceName(addressResolverOptionName));
    }
    Supplier<EventBusOptions> eventBusOptionsSupplier = null;
    if (eventBusOptionName != null) {
      eventBusOptionsSupplier = vertxServiceBuilder.requires(EventBusResourceDefinition.VERTX_EVENT_BUS_OPTIONS_CAPABILITY.getCapabilityServiceName(eventBusOptionName));
    }
    Supplier<MetricsOptions> metricsOptionsSupplier = null;
    if (metricsOptionName != null) {
      metricsOptionsSupplier = vertxServiceBuilder.requires(ServiceName.parse("TODO"));
    }
    Supplier<TracingOptions> tracingOptionsSupplier = null;
    if (tracingOptionName != null) {
      tracingOptionsSupplier = vertxServiceBuilder.requires(ServiceName.parse("TODO"));
    }
    vertxServiceBuilder.setInstance(new NamedVertxOptionsService(namedVertxOptions, serverEnvSupplier,
      addressResolverOptionsSupplier, eventBusOptionsSupplier, metricsOptionsSupplier, tracingOptionsSupplier, consumer));
    vertxServiceBuilder
      .setInitialMode(ServiceController.Mode.ACTIVE)
      .install();
  }

  @Override
  public void start(StartContext startContext) throws StartException {
    this.namedVertxOptions.getVertxOptions().getFileSystemOptions().setFileCacheDir(serverEnvSupplier.get().getServerTempDir() + File.separator + "vertx-cache");
    if (this.addressResolverOptionsSupplier != null && this.addressResolverOptionsSupplier.get() != null) {
      this.namedVertxOptions.getVertxOptions().setAddressResolverOptions(this.addressResolverOptionsSupplier.get());
    }
    if (this.eventBusOptionsSupplier != null && this.eventBusOptionsSupplier.get() != null) {
      this.namedVertxOptions.getVertxOptions().setEventBusOptions(this.eventBusOptionsSupplier.get());
    }
    if (this.metricsOptionsSupplier != null && this.metricsOptionsSupplier.get() != null) {
      this.namedVertxOptions.getVertxOptions().setMetricsOptions(this.metricsOptionsSupplier.get());
    }
    if (this.tracingOptionsSupplier != null && this.tracingOptionsSupplier.get() != null) {
      this.namedVertxOptions.getVertxOptions().setTracingOptions(this.tracingOptionsSupplier.get());
    }
    this.consumer.accept(this.namedVertxOptions);
    VertxOptionsRegistry.getInstance().addVertxOptions(this.namedVertxOptions);
  }

  @Override
  public void stop(StopContext stopContext) {
    VertxOptionsRegistry.getInstance().removeVertxOptions(this.namedVertxOptions.getName());
  }

}
