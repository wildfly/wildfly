/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component.stateless;

import org.jboss.as.ee.component.AbstractComponent;
import org.jboss.as.ee.component.AbstractComponentInstance;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.component.EJBComponentConfiguration;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactoryContext;

import java.util.List;

/**
 * {@link org.jboss.as.ee.component.Component} responsible for managing EJB3 stateless session beans
 * <p/>
 * <p/>
 * Author : Jaikiran Pai
 */
public class StatelessSessionComponent extends AbstractComponent {


    /**
     * The component level interceptors that will be applied during the invocation
     * on the bean
     */
    private List<Interceptor> componentInterceptors;

    // some more injectable resources
    // @Resource
    // private Pool pool;

    /**
     * Constructs a StatelessEJBComponent for a stateless session bean
     *
     * @param componentConfiguration
     */
    public StatelessSessionComponent(final EJBComponentConfiguration componentConfiguration) {
        this(componentConfiguration, null);
    }

    /**
     * Constructs a StatelessEJBComponent for a stateless session bean
     *
     * @param componentConfiguration
     * @param componentInterceptors
     */
    public StatelessSessionComponent(final EJBComponentConfiguration componentConfiguration, List<Interceptor> componentInterceptors) {
        super(componentConfiguration);
        this.componentInterceptors = componentInterceptors;
    }

    // TODO: I need to understand what exactly is this method meant for
    @Override
    public Interceptor createClientInterceptor(Class<?> viewClass) {
        // TODO: Needs to be implemented
        return new Interceptor() {

            @Override
            public Object processInvocation(InterceptorContext context) throws Exception {
                // TODO: FIXME: Component shouldn't be attached in a interceptor context that
                // runs on remote clients.
                context.putPrivateData(Component.class, StatelessSessionComponent.this);
                return context.proceed();
            }
        };
    }

    //TODO: This should be getInstance()
    @Override
    public ComponentInstance createInstance() {
        // TODO: Use a pool
        return super.createInstance();
    }

    @Override
    protected AbstractComponentInstance constructComponentInstance(Object instance, List<Interceptor> preDestroyInterceptors, InterceptorFactoryContext context) {
        return new StatelessSessionComponentInstance(this, instance, preDestroyInterceptors, context);
    }


}
