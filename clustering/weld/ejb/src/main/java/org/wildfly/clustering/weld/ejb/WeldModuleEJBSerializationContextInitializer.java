/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.weld.ejb;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.PrivilegedAction;

import org.infinispan.protostream.SerializationContext;
import org.jboss.weld.ejb.api.SessionObjectReference;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.module.ejb.EnterpriseBeanInstance;
import org.jboss.weld.serialization.spi.BeanIdentifier;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.reflect.TernaryFieldMarshaller;
import org.wildfly.clustering.marshalling.protostream.reflect.TriFunction;
import org.wildfly.security.ParametricPrivilegedAction;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
public class WeldModuleEJBSerializationContextInitializer extends AbstractSerializationContextInitializer implements ParametricPrivilegedAction<Class<?>, String> {

    public WeldModuleEJBSerializationContextInitializer() {
        super("org.jboss.weld.module.ejb.proto");
    }

    @Override
    public Class<?> run(String className) {
        try {
            return EnterpriseBeanInstance.class.getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(className);
        }
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        // Gotta love overly restrictive modifiers...
        Class<?> methodHandlerClass = WildFlySecurityManager.doUnchecked("org.jboss.weld.module.ejb.EnterpriseBeanProxyMethodHandler", this);
        Class<?> sessionBeanImplClass = WildFlySecurityManager.doUnchecked("org.jboss.weld.module.ejb.SessionBeanImpl", this);
        TriFunction<BeanManagerImpl, BeanIdentifier, SessionObjectReference, Object> function = new TriFunction<>() {
            @Override
            public Object apply(BeanManagerImpl manager, BeanIdentifier identifier, SessionObjectReference reference) {
                return WildFlySecurityManager.doUnchecked(new PrivilegedAction<Object>() {
                    @Override
                    public Object run() {
                        try {
                            Constructor<?> constructor = methodHandlerClass.getDeclaredConstructor(sessionBeanImplClass, SessionObjectReference.class);
                            constructor.setAccessible(true);
                            return constructor.newInstance(manager.getPassivationCapableBean(identifier), reference);
                        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                });
            }
        };
        context.registerMarshaller(new TernaryFieldMarshaller<>(methodHandlerClass, BeanManagerImpl.class, BeanIdentifier.class, SessionObjectReference.class, function));
    }
}
