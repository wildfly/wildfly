/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.component.interceptors;

import javax.ejb.EJBHome;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.ejb.client.EJBMetaDataImpl;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Interceptor that handles the EJB metadata for non-IIOP invocations.
 *
 * @author Stuart Douglas
 */
public class EjbMetadataInterceptorFactory implements InterceptorFactory {

    private final InjectedValue<ComponentView> homeView = new InjectedValue<ComponentView>();

    private final Interceptor interceptor;

    public EjbMetadataInterceptorFactory(final Class<?> remoteClass, final Class<?> homeClass, final Class<?> pkClass, final boolean session, final boolean stateless) {
        this.interceptor = new Interceptor() {
            @Override
            public Object processInvocation(final InterceptorContext context) throws Exception {
                return new EJBMetaDataImpl(remoteClass, homeClass, pkClass, session, stateless, (EJBHome) homeView.getValue().createInstance().getInstance());
            }
        };
    }

    @Override
    public Interceptor create(final InterceptorFactoryContext context) {
        return interceptor;
    }

    public InjectedValue<ComponentView> getHomeView() {
        return homeView;
    }
}
