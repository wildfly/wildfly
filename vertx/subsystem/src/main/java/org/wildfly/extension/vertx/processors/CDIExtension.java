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
package org.wildfly.extension.vertx.processors;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.inject.Singleton;
import org.wildfly.extension.vertx.VertxProxyHolder;

/**
 * CDI Extension which adds the ability to inject the Vertx instances by the member name.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class CDIExtension implements Extension {

    public CDIExtension() {
    }

    public void registerRawVertxBean(@Observes AfterBeanDiscovery afterBeanDiscovery) {
        afterBeanDiscovery.addBean()
                .scope(Singleton.class)
                .types(io.vertx.core.Vertx.class)
                .beanClass(io.vertx.core.Vertx.class)
                .createWith(
                        creationalContext -> VertxProxyHolder.instance().getVertxProxy() != null ?
                                VertxProxyHolder.instance().getVertxProxy().getVertx() :
                                io.vertx.core.Vertx.vertx());
    }

    public void registerMutinyVertxBean(@Observes AfterBeanDiscovery afterBeanDiscovery) {
        afterBeanDiscovery.addBean()
                .scope(Singleton.class)
                .types(io.vertx.mutiny.core.Vertx.class)
                .beanClass(io.vertx.mutiny.core.Vertx.class)
                .createWith(
                        creationalContext -> VertxProxyHolder.instance().getVertxProxy() != null ?
                                new io.vertx.mutiny.core.Vertx(VertxProxyHolder.instance().getVertxProxy().getVertx()) :
                                io.vertx.mutiny.core.Vertx.vertx());
    }

}
