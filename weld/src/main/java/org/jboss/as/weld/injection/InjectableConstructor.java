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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;

import org.jboss.weld.manager.BeanManagerImpl;

/**
 * @author Stuart Douglas
 */
public class InjectableConstructor {

    private final Constructor constructor;
    private final ParameterInjectionPoint[] parameterInjectionPoints;
    private final Bean[] beans;
    private final BeanManagerImpl beanManager;


    public InjectableConstructor(AnnotatedConstructor<?> constructor, BeanManagerImpl beanManager, Bean bean) {
        this.constructor = constructor.getJavaMember();
        SecurityActions.setAccessible(this.constructor);
        this.parameterInjectionPoints = new ParameterInjectionPoint[constructor.getParameters().size()];
        this.beans = new Bean[parameterInjectionPoints.length];
        this.beanManager = beanManager;

        for(AnnotatedParameter<?> parameter : constructor.getParameters()) {
            final Set<Annotation> qualifiers = new HashSet<Annotation>();
            for(Annotation annotation : parameter.getAnnotations()) {
                if(beanManager.isQualifier(annotation.annotationType())) {
                    qualifiers.add(annotation);
                }
            }
            ParameterInjectionPoint injectionPoint = new ParameterInjectionPoint(parameter, qualifiers, bean);
            final Set<Bean<?>> ipBeans = beanManager.getBeans(injectionPoint);
            final Bean<?> ipBean = beanManager.resolve(ipBeans);
            parameterInjectionPoints[parameter.getPosition()] = injectionPoint;
            beans[parameter.getPosition()] = ipBean;
        }
    }

    public Object createInstance(CreationalContext<?> ctx) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        final Object[] params = new Object[beans.length];
        for(int i = 0; i < beans.length; ++i) {
            params[i] = beanManager.getReference(parameterInjectionPoints[i],beans[i],ctx);
        }
        return constructor.newInstance(params);
    }
}
