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
package org.jboss.as.weld.injection;

import org.jboss.weld.manager.BeanManagerImpl;

import javax.enterprise.context.spi.CreationalContext;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Class that knows how to create and inject a class that requires CDI injection
 *
 * @author Stuart Douglas
 */
class WeldEEInjection {

    private final Class<?> componentClass;
    private final BeanManagerImpl beanManager;
    private final List<InjectableField> injectableFields;
    private final List<InjectableMethod> injectableMethods;
    private final InjectableConstructor constructor;

    public WeldEEInjection(Class<?> componentClass, BeanManagerImpl beanManager, List<InjectableField> injectableFields, List<InjectableMethod> injectableMethods, InjectableConstructor constructor) {
        this.componentClass = componentClass;
        this.beanManager = beanManager;
        this.injectableFields = injectableFields;
        this.injectableMethods = injectableMethods;
        this.constructor = constructor;
    }

    /**
     * Run field and method injections. Resource injections should be performed before this method is called
     *
     * @param instance The instance to inject
     * @param ctx      The creational context that was used to create the instance
     */
    public void inject(Object instance, CreationalContext<?> ctx) {
        for (InjectableField field : injectableFields) {
            field.inject(instance, beanManager, ctx);
        }
        for (InjectableMethod method : injectableMethods) {
            method.inject(instance, beanManager, ctx);
        }
    }

    /**
     * Create an instance of the class by calling the bean constructor
     *
     * @param ctx The creational context to use
     * @return A new instance of the object
     */
    public Object produce(CreationalContext<?> ctx) {

        try {
            if (constructor != null) {
                return constructor.createInstance(ctx);
            } else {
                return componentClass.newInstance();
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
}
