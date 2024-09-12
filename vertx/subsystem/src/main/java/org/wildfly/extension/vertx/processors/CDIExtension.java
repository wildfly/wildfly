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

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InjectionTarget;
import jakarta.enterprise.inject.spi.InjectionTargetFactory;
import org.wildfly.extension.vertx.VertxProxyHolder;
import org.wildfly.extension.vertx.logging.VertxLogger;

import java.util.Set;

/**
 * CDI Extension which adds the ability to inject the Vertx instances by the member name.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class CDIExtension implements Extension {

    public CDIExtension() {
    }

    public void registerVertxBean(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {
        if (VertxProxyHolder.instance().getVertxProxy() != null) {
            VertxLogger.VERTX_LOGGER.useVertxFromSubsystem();
            AnnotatedType<io.vertx.core.Vertx> rawVertxAnnotatedType = beanManager.createAnnotatedType(io.vertx.core.Vertx.class);
            BeanAttributes<io.vertx.core.Vertx> rawVertxBeanAttributes = beanManager.createBeanAttributes(rawVertxAnnotatedType);
            afterBeanDiscovery.addBean(beanManager.createBean(rawVertxBeanAttributes, io.vertx.core.Vertx.class, new RawVertxProducer()));

            AnnotatedType<io.vertx.mutiny.core.Vertx> annotatedType = beanManager.createAnnotatedType(io.vertx.mutiny.core.Vertx.class);
            BeanAttributes<io.vertx.mutiny.core.Vertx> beanAttributes = beanManager.createBeanAttributes(annotatedType);
            afterBeanDiscovery.addBean(beanManager.createBean(beanAttributes, io.vertx.mutiny.core.Vertx.class, new MunityVertxProducer()));
        }
    }

    private class MunityVertxProducer implements InjectionTargetFactory<io.vertx.mutiny.core.Vertx> {
        @Override
        public InjectionTarget<io.vertx.mutiny.core.Vertx> createInjectionTarget(Bean<io.vertx.mutiny.core.Vertx> bean) {
            return new InjectionTarget<>() {
                @Override
                public void inject(io.vertx.mutiny.core.Vertx instance, CreationalContext<io.vertx.mutiny.core.Vertx> ctx) {
                }

                @Override
                public void postConstruct(io.vertx.mutiny.core.Vertx instance) {
                }

                @Override
                public void preDestroy(io.vertx.mutiny.core.Vertx instance) {
                }

                @Override
                public io.vertx.mutiny.core.Vertx produce(CreationalContext<io.vertx.mutiny.core.Vertx> ctx) {
                    return mutinyVertx();
                }

                @Override
                public void dispose(io.vertx.mutiny.core.Vertx instance) {
                }

                @Override
                public Set<InjectionPoint> getInjectionPoints() {
                    return Set.of();
                }
            };
        }
    }
    private class RawVertxProducer implements InjectionTargetFactory<io.vertx.core.Vertx> {
        @Override
        public InjectionTarget<io.vertx.core.Vertx> createInjectionTarget(Bean<io.vertx.core.Vertx> bean) {
            return new InjectionTarget<>() {
                @Override
                public void inject(io.vertx.core.Vertx instance, CreationalContext<io.vertx.core.Vertx> ctx) {
                }

                @Override
                public void postConstruct(io.vertx.core.Vertx instance) {
                }

                @Override
                public void preDestroy(io.vertx.core.Vertx instance) {
                }

                @Override
                public io.vertx.core.Vertx produce(CreationalContext<io.vertx.core.Vertx> ctx) {
                    return rawVertx();
                }

                @Override
                public void dispose(io.vertx.core.Vertx instance) {
                }

                @Override
                public Set<InjectionPoint> getInjectionPoints() {
                    return Set.of();
                }
            };
        }
    }

    private io.vertx.mutiny.core.Vertx mutinyVertx() {
        return VertxProxyHolder.instance().getVertxProxy().getMutiyVertx();
    }

    private io.vertx.core.Vertx rawVertx() {
        return VertxProxyHolder.instance().getVertxProxy().getVertx();
    }
}
