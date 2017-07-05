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
import java.util.Set;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.jberet.creation.AbstractArtifactFactory;
import org.jberet.spi.ArtifactFactory;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.batch.jberet._private.BatchLogger;

/**
 * ArtifactFactory for Java EE runtime environment.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ArtifactFactoryService extends AbstractArtifactFactory implements Service<ArtifactFactory> {
    private final InjectedValue<BeanManager> beanManagerInjector = new InjectedValue<>();

    private final Map<Object, CreationalContext<?>> contexts = Collections.synchronizedMap(new HashMap<>());
    private volatile BeanManager beanManager;

    @Override
    public void destroy(final Object instance) {
        final CreationalContext<?> context = contexts.remove(instance);
        if (context == null) {
            super.destroy(instance);
        } else {
            context.release();
        }
    }

    @Override
    public Class<?> getArtifactClass(final String ref, final ClassLoader classLoader) {
        final BeanManager beanManager = this.beanManager;
        final Bean<?> bean = getBean(ref, beanManager);
        return bean == null ? null : bean.getBeanClass();
    }

    @Override
    public Object create(final String ref, Class<?> cls, final ClassLoader classLoader) throws Exception {
        final BeanManager beanManager = this.beanManager;
        final Bean<?> bean = getBean(ref, beanManager);
        if (bean == null) {
            return null;
        }
        final CreationalContext<?> context = beanManager.createCreationalContext(bean);
        final Object result = beanManager.getReference(bean, bean.getBeanClass(), context);
        contexts.put(result, context);
        return result;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        beanManager = beanManagerInjector.getOptionalValue();
    }

    @Override
    public void stop(final StopContext context) {
        beanManager = null;
        synchronized (contexts) {
            for (Object key : contexts.keySet()) {
                final CreationalContext<?> creationalContext = contexts.remove(key);
                creationalContext.release();
            }
        }
    }

    @Override
    public ArtifactFactory getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<BeanManager> getBeanManagerInjector() {
        return beanManagerInjector;
    }

    private static Bean<?> getBean(final String ref, final BeanManager beanManager) {
        if (beanManager == null) {
            return null;
        }
        BatchLogger.LOGGER.tracef("Looking up bean reference for '%s'", ref);
        final Set<Bean<?>> beans = beanManager.getBeans(ref);
        final Bean<?> bean = beanManager.resolve(beans);
        if (bean != null) {
            BatchLogger.LOGGER.tracef("Found bean '%s' for reference '%s'", bean, ref);
        } else {
            BatchLogger.LOGGER.tracef("No bean found for reference '%s;'", ref);
        }
        return bean;
    }
}
