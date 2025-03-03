/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.ejb;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.PrivilegedAction;

import org.jboss.weld.ejb.api.SessionObjectReference;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.module.ejb.EnterpriseBeanInstance;
import org.jboss.weld.module.ejb.WeldEjbModule;
import org.jboss.weld.serialization.spi.BeanIdentifier;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.reflect.TernaryFieldMarshaller;
import org.wildfly.clustering.marshalling.protostream.reflect.TriFunction;
import org.wildfly.security.ParametricPrivilegedAction;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class WeldModuleEJBSerializationContextInitializer extends AbstractSerializationContextInitializer implements ParametricPrivilegedAction<Class<?>, String> {

    public WeldModuleEJBSerializationContextInitializer() {
        super(WeldEjbModule.class.getPackage());
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
                return WildFlySecurityManager.doUnchecked(new PrivilegedAction<>() {
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
