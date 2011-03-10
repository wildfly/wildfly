/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.component.session.stateful;

import org.jboss.as.ee.component.AbstractComponent;
import org.jboss.as.ee.component.AbstractComponentInstance;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ejb3.cache.NoPassivationCache;
import org.jboss.as.ejb3.cache.spi.Cache;
import org.jboss.as.ejb3.cache.spi.StatefulObjectFactory;
import org.jboss.as.ejb3.component.EJBComponentConfiguration;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactoryContext;

import java.io.Serializable;
import java.util.List;

/**
 * Stateful Session Bean
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class StatefulSessionComponent extends AbstractComponent {
    private Cache<StatefulSessionComponentInstance> cache;

    /**
     * Construct a new instance.
     *
     * @param configuration         the component configuration
     */
    protected StatefulSessionComponent(final EJBComponentConfiguration configuration) {
        super(configuration);

        cache = new NoPassivationCache<StatefulSessionComponentInstance>();
        cache.setStatefulObjectFactory(new StatefulObjectFactory<StatefulSessionComponentInstance>() {
            @Override
            public StatefulSessionComponentInstance createInstance() {
                return (StatefulSessionComponentInstance) StatefulSessionComponent.this.createInstance();
            }

            @Override
            public void destroyInstance(StatefulSessionComponentInstance instance) {
                StatefulSessionComponent.this.destroyInstance(instance);
            }
        });
    }

    @Override
    public Interceptor createClientInterceptor(Class<?> view) {
        final Serializable sessionId = createSession();
        return new Interceptor() {
            @Override
            public Object processInvocation(InterceptorContext context) throws Exception {
                // TODO: attaching as Serializable.class is a bit wicked
                context.putPrivateData(Serializable.class, sessionId);
                // TODO: this won't work for remote proxies
                context.putPrivateData(Component.class, StatefulSessionComponent.this);
                try {
                    return context.proceed();
                }
                finally {
                    // TODO: how do I remove it?
                    //context.removePrivateData(Serializable.class);
                }
            }
        };
    }

    private Serializable createSession() {
        return getCache().create().getId();
    }

    protected Cache<StatefulSessionComponentInstance> getCache() {
        return cache;
    }

    @Override
    protected AbstractComponentInstance constructComponentInstance(Object instance, List<Interceptor> preDestroyInterceptors, InterceptorFactoryContext context) {
        return new StatefulSessionComponentInstance(this, instance, preDestroyInterceptors, context);
    }
}
