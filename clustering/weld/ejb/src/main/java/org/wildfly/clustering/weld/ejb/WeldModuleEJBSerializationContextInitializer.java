/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.ejb;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.jboss.weld.ejb.api.SessionObjectReference;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.module.ejb.EnterpriseBeanInstance;
import org.jboss.weld.module.ejb.WeldEjbModule;
import org.jboss.weld.serialization.spi.BeanIdentifier;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.function.BiFunction;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.reflect.TernaryFieldMarshaller;
import org.wildfly.clustering.marshalling.protostream.reflect.TriFunction;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class WeldModuleEJBSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public WeldModuleEJBSerializationContextInitializer() {
        super(WeldEjbModule.class.getPackage());
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        try {
            // Gotta love overly restrictive modifiers...
            @SuppressWarnings("unchecked")
            Class<Object> methodHandlerClass = (Class<Object>) EnterpriseBeanInstance.class.getClassLoader().loadClass("org.jboss.weld.module.ejb.EnterpriseBeanProxyMethodHandler");
            Class<?> sessionBeanImplClass = EnterpriseBeanInstance.class.getClassLoader().loadClass("org.jboss.weld.module.ejb.SessionBeanImpl");
            MethodHandle constructorHandle = MethodHandles.privateLookupIn(methodHandlerClass, MethodHandles.lookup()).findConstructor(methodHandlerClass, MethodType.methodType(void.class, sessionBeanImplClass, SessionObjectReference.class));
            TriFunction<BeanManagerImpl, BeanIdentifier, SessionObjectReference, Object> function = new TriFunction<>() {
                @Override
                public Object apply(BeanManagerImpl manager, BeanIdentifier identifier, SessionObjectReference reference) {
                    return BiFunction.invoke(constructorHandle).apply(manager.getPassivationCapableBean(identifier), reference);
                }
            };
            context.registerMarshaller(new TernaryFieldMarshaller<>(methodHandlerClass, BeanManagerImpl.class, BeanIdentifier.class, SessionObjectReference.class, function));
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }
}
