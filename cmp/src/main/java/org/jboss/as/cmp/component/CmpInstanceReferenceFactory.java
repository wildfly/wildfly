/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.cmp.component;

import javax.ejb.EntityBean;
import org.jboss.as.cmp.CmpMessages;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.InjectedValue;

/**
 * @author John Bailey
 */
public class CmpInstanceReferenceFactory implements ManagedReferenceFactory {
    private Class<?> beanClass;
    private final InjectedValue<CmpEntityBeanComponent> component = new InjectedValue<CmpEntityBeanComponent>();
    private final ProxyFactory<?> proxyFactory;

    public CmpInstanceReferenceFactory(final Class<?> beanClass, final Class<?>... viewClasses) {
        this.beanClass = beanClass;
        proxyFactory = CmpProxyFactory.createProxyFactory((Class<EntityBean>)beanClass, viewClasses);
    }

    public ManagedReference getReference() {
        final Object proxy;
        try {
            proxy = proxyFactory.newInstance(new CmpEntityBeanInvocationHandler(component.getValue()));
        } catch (Exception e) {
            throw CmpMessages.MESSAGES.failedToCreateProxyInstance(beanClass, e);
        }
        return new ManagedReference() {
            public void release() {
            }

            public Object getInstance() {
                return proxy;
            }
        };
    }

    public Injector<CmpEntityBeanComponent> getComponentInjector() {
        return component;
    }
}
