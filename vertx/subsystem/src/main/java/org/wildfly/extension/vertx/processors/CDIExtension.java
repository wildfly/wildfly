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

import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InjectionTarget;
import jakarta.enterprise.inject.spi.InjectionTargetFactory;
import org.wildfly.extension.vertx.VertxProxy;
import org.wildfly.extension.vertx.VertxProxyHolder;
import org.wildfly.extension.vertx.logging.VertxLogger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import static org.wildfly.extension.vertx.VertxConstants.CDI_QUALIFIER;

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
            // Expose the Bean with @Any and @Identifier qualifiers, client side needs to use the same Qualifiers to select this instance.
            final Set<Annotation> qualifiers = Set.of(Any.Literal.INSTANCE, Identifier.Literal.of(CDI_QUALIFIER));
            AnnotatedType<io.vertx.core.Vertx> rawAnnotatedType = beanManager.createAnnotatedType(io.vertx.core.Vertx.class);
            BeanAttributes<io.vertx.core.Vertx> rawBeanAttributes =
                    new BeanAttributesWrapper<>(beanManager.createBeanAttributes(rawAnnotatedType), qualifiers);
            afterBeanDiscovery.addBean(beanManager.createBean(rawBeanAttributes, io.vertx.core.Vertx.class, new AbstractVertxProducer<>() {
                @Override
                protected io.vertx.core.Vertx produceBeanObject(CreationalContext<io.vertx.core.Vertx> ctx) {
                    return rawVertx();
                }
            }));

            AnnotatedType<io.vertx.mutiny.core.Vertx> mutinyAnnotatedType = beanManager.createAnnotatedType(io.vertx.mutiny.core.Vertx.class);
            BeanAttributes<io.vertx.mutiny.core.Vertx> mutinyBeanAttributes =
                    new BeanAttributesWrapper<>(beanManager.createBeanAttributes(mutinyAnnotatedType), qualifiers);
            afterBeanDiscovery.addBean(beanManager.createBean(mutinyBeanAttributes, io.vertx.mutiny.core.Vertx.class, new AbstractVertxProducer<>() {
                @Override
                protected io.vertx.mutiny.core.Vertx produceBeanObject(CreationalContext<io.vertx.mutiny.core.Vertx> ctx) {
                    return mutinyVertx();
                }
            }));
        }
    }

    private static class BeanAttributesWrapper<T> implements BeanAttributes<T> {
        private final BeanAttributes<T> delegate;
        private final Set<Annotation> qualifiers;
        BeanAttributesWrapper(BeanAttributes<T> delegate, Set<Annotation> qualifiers) {
            this.delegate = delegate;
            this.qualifiers = new HashSet<>(qualifiers);
        }

        @Override
        public Set<Type> getTypes() {
            return delegate.getTypes();
        }

        @Override
        public Set<Annotation> getQualifiers() {
            return qualifiers;
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return delegate.getScope();
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public Set<Class<? extends Annotation>> getStereotypes() {
            return delegate.getStereotypes();
        }

        @Override
        public boolean isAlternative() {
            return delegate.isAlternative();
        }

    }

    private abstract static class AbstractVertxProducer<T> implements InjectionTargetFactory<T> {

        @Override
        public InjectionTarget<T> createInjectionTarget(Bean<T> bean) {
            return new InjectionTarget<>() {

                @Override
                public T produce(CreationalContext<T> ctx) {
                    return produceBeanObject(ctx);
                }

                @Override
                public void dispose(T instance) {
                }

                @Override
                public Set<InjectionPoint> getInjectionPoints() {
                    return Set.of();
                }

                @Override
                public void inject(T instance, CreationalContext<T> ctx) {
                }

                @Override
                public void postConstruct(T instance) {
                }

                @Override
                public void preDestroy(T instance) {
                }
            };
        }

        protected abstract T produceBeanObject(CreationalContext<T> ctx);

    }

    private static io.vertx.mutiny.core.Vertx mutinyVertx() {
        VertxProxy vertxProxy = VertxProxyHolder.instance().getVertxProxy();
        if (vertxProxy == null) {
            throw VertxLogger.VERTX_LOGGER.noVertxDefined();
        }
        VertxLogger.VERTX_LOGGER.useVertxFromSubsystem();
        return vertxProxy.getMutiyVertx();
    }

    private static io.vertx.core.Vertx rawVertx() {
        VertxProxy vertxProxy = VertxProxyHolder.instance().getVertxProxy();
        if (vertxProxy == null) {
            throw VertxLogger.VERTX_LOGGER.noVertxDefined();
        }
        VertxLogger.VERTX_LOGGER.useVertxFromSubsystem();
        return vertxProxy.getVertx();
    }
}