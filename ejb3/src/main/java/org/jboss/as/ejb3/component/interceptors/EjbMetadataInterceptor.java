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
import javax.ejb.EJBObject;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EntityEJBMetaData;
import org.jboss.ejb.client.StatefulEJBMetaData;
import org.jboss.ejb.client.StatelessEJBMetaData;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Interceptor that handles the EJB metadata for non-IIOP invocations.
 *
 * @author Stuart Douglas
 */
public class EjbMetadataInterceptor implements Interceptor {

    private final InjectedValue<ComponentView> homeView = new InjectedValue<ComponentView>();
    private final Class<?> remoteClass;
    private final Class<? extends EJBHome> homeClass;
    private final Class<?> pkClass;
    private final boolean session;
    private final boolean stateless;

    public EjbMetadataInterceptor(Class<?> remoteClass, Class<? extends EJBHome> homeClass, Class<?> pkClass, boolean session, boolean stateless) {
        this.remoteClass = remoteClass;
        this.homeClass = homeClass;
        this.pkClass = pkClass;
        this.session = session;
        this.stateless = stateless;
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        final EJBHome home = (EJBHome) homeView.getValue().createInstance().getInstance();
        if (session) {
            if (stateless) {
                return createStatelessMetaData(remoteClass.asSubclass(EJBObject.class), homeClass, home);
            } else {
                return createStatefulMetaData(remoteClass.asSubclass(EJBObject.class), homeClass, home);
            }
        } else {
            return createEntityMetaData(remoteClass.asSubclass(EJBObject.class), homeClass, home, pkClass);
        }
    }

    private static <T extends EJBObject, H extends EJBHome> StatelessEJBMetaData<T, ? extends H> createStatelessMetaData(Class<T> remoteClass, Class<H> homeClass, EJBHome home) {
        return new StatelessEJBMetaData<>(remoteClass, EJBClient.getLocatorFor(home).<H>narrowAsHome(homeClass));
    }

    private static <T extends EJBObject, H extends EJBHome> StatefulEJBMetaData<T, ? extends H> createStatefulMetaData(Class<T> remoteClass, Class<H> homeClass, EJBHome home) {
        return new StatefulEJBMetaData<>(remoteClass, EJBClient.getLocatorFor(home).<H>narrowAsHome(homeClass));
    }

    private static <T extends EJBObject, H extends EJBHome> EntityEJBMetaData<T, ? extends H> createEntityMetaData(Class<T> remoteClass, Class<H> homeClass, EJBHome home, Class<?> pkClass) {
        return new EntityEJBMetaData<>(remoteClass, EJBClient.getLocatorFor(home).<H>narrowAsHome(homeClass), pkClass);
    }

    public InjectedValue<ComponentView> getHomeView() {
        return homeView;
    }
}
