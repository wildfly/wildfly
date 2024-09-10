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

import io.vertx.core.dns.AddressResolverOptions;
import org.jboss.as.controller.OperationContext;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class NamedVertxOptionsService implements Service {

  private final NamedVertxOptions namedVertxOptions;
  private final Consumer<NamedVertxOptions> consumer;
  private final Supplier<AddressResolverOptions> addressResolverOptionsSupplier;

  NamedVertxOptionsService(NamedVertxOptions namedVertxOptions, Consumer<NamedVertxOptions> consumer) {
    this(namedVertxOptions, null, consumer);
  }

  NamedVertxOptionsService(NamedVertxOptions namedVertxOptions,
                           Supplier<AddressResolverOptions> addressResolverOptionsSupplier,
                           Consumer<NamedVertxOptions> consumer) {
    this.namedVertxOptions = namedVertxOptions;
    this.addressResolverOptionsSupplier = addressResolverOptionsSupplier;
    this.consumer = consumer;
  }

  static void installService(OperationContext context, NamedVertxOptions namedVertxOptions,
                             String addressResolverOptionName) {
    ServiceName vertxServiceName = VertxOptionFileResourceDefinition.VERTX_OPTIONS_CAPABILITY.getCapabilityServiceName(namedVertxOptions.getName());
    ServiceBuilder<?> vertxServiceBuilder = context.getCapabilityServiceTarget().addService();
    Consumer<NamedVertxOptions> consumer = vertxServiceBuilder.provides(vertxServiceName);
    Supplier<AddressResolverOptions> addressResolverOptionsSupplier = null;
    if (addressResolverOptionName != null) {
      addressResolverOptionsSupplier = vertxServiceBuilder.requires(AddressResolverResourceDefinition.VERTX_OPTIONS_ADDRESS_RESOLVER_CAPABILITY.getCapabilityServiceName(addressResolverOptionName));
    }
    vertxServiceBuilder.setInstance(new NamedVertxOptionsService(namedVertxOptions, addressResolverOptionsSupplier, consumer));
    vertxServiceBuilder
      .setInitialMode(ServiceController.Mode.ACTIVE)
      .install();
  }

  @Override
  public void start(StartContext startContext) throws StartException {
    if (this.addressResolverOptionsSupplier != null && this.addressResolverOptionsSupplier.get() != null) {
      this.namedVertxOptions.getVertxOptions().setAddressResolverOptions(this.addressResolverOptionsSupplier.get());
    }
    this.consumer.accept(this.namedVertxOptions);
    VertxOptionsRegistry.getInstance().addVertxOptions(this.namedVertxOptions);
  }

  @Override
  public void stop(StopContext stopContext) {
    VertxOptionsRegistry.getInstance().removeVertxOptions(this.namedVertxOptions.getName());
  }

}
