/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.batch.jberet.deployment;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

import org.jberet.creation.AbstractArtifactFactory;
import org.jberet.spi.ArtifactFactory;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.context.RequestContext;
import org.jboss.weld.context.unbound.UnboundLiteral;
import org.jboss.weld.manager.BeanManagerImpl;
import org.wildfly.extension.batch.jberet._private.BatchLogger;

/**
 * ArtifactFactory for Jakarta EE runtime environment.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class ArtifactFactoryService extends AbstractArtifactFactory implements Service, WildFlyArtifactFactory {
    private final Consumer<ArtifactFactory> artifactFactoryConsumer;
    private final Supplier<BeanManager> beanManagerSupplier;

    private final Map<Object, Holder> contexts = Collections.synchronizedMap(new HashMap<>());
    private volatile BeanManager beanManager;

    public ArtifactFactoryService(final Consumer<ArtifactFactory> artifactFactoryConsumer,
                                  final Supplier<BeanManager> beanManagerSupplier) {
        this.artifactFactoryConsumer = artifactFactoryConsumer;
        this.beanManagerSupplier = beanManagerSupplier;
    }

    @Override
    public void destroy(final Object instance) {
        final Holder holder = contexts.remove(instance);
        if (holder == null) {
            // This bean was not created via Jakarta Contexts and Dependency Injection, we need to invoke the JBeret cleanup
            super.destroy(instance);
        } else {
            // Only release the context for @Dependent beans, Weld should take care of the other scopes
            if (holder.isDependent()) {
                // We're not destroying the bean here because weld should handle that for use when we release the
                // CreationalContext
                holder.context.release();
            }
        }
    }

    @Override
    public Class<?> getArtifactClass(final String ref, final ClassLoader classLoader) {
        final Bean<?> bean = getBean(ref, getBeanManager(), classLoader);
        return bean == null ? null : bean.getBeanClass();
    }

    @Override
    public Object create(final String ref, Class<?> cls, final ClassLoader classLoader) throws Exception {
        final BeanManager beanManager = getBeanManager();
        final Bean<?> bean = getBean(ref, beanManager, classLoader);
        if (bean == null) {
            return null;
        }
        final CreationalContext<?> context = beanManager.createCreationalContext(bean);
        final Object result = beanManager.getReference(bean, bean.getBeanClass(), context);
        contexts.put(result, new Holder(bean, context));
        return result;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        beanManager = beanManagerSupplier != null ? beanManagerSupplier.get() : null;
        artifactFactoryConsumer.accept(this);
    }

    @Override
    public void stop(final StopContext context) {
        artifactFactoryConsumer.accept(null);
        beanManager = null;
        synchronized (contexts) {
            for (Holder holder : contexts.values()) {
                // Only release the context for @Dependent beans, Weld should take care of the other scopes
                if (holder.isDependent()) {
                    holder.context.release();
                }
            }
            contexts.clear();
        }
    }

    @Override
    public ContextHandle createContextHandle() {
        final BeanManagerImpl beanManager = getBeanManager();
        return () -> {
            if (beanManager == null || beanManager.isContextActive(RequestScoped.class)) {
                return () -> {
                };
            }
            final RequestContext requestContext = beanManager.instance().select(RequestContext.class, UnboundLiteral.INSTANCE).get();
            requestContext.activate();
            return () -> {
                requestContext.invalidate();
                requestContext.deactivate();
            };
        };
    }

    private BeanManagerImpl getBeanManager() {
        final BeanManager beanManager = this.beanManager;
        return beanManager == null ? null : BeanManagerProxy.unwrap(beanManager);
    }

    private static Bean<?> getBean(final String ref, final BeanManager beanManager, final ClassLoader classLoader) {
        final Bean<?> result = beanManager == null ? null : AbstractArtifactFactory.findBean(ref, beanManager, classLoader);
        BatchLogger.LOGGER.tracef("Found bean: %s for ref: %s", result, ref);
        return result;
    }

    private static class Holder {
        final Bean<?> bean;
        final CreationalContext<?> context;

        private Holder(final Bean<?> bean, final CreationalContext<?> context) {
            this.bean = bean;
            this.context = context;
        }

        boolean isDependent() {
            return Dependent.class.equals(bean.getScope());
        }
    }
}
