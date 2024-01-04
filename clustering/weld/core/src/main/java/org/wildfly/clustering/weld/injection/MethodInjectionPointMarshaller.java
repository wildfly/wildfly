/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.injection;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.PrivilegedAction;
import java.util.Collections;

import jakarta.enterprise.inject.spi.Bean;

import org.infinispan.protostream.descriptors.WireType;
import org.jboss.weld.Container;
import org.jboss.weld.annotated.enhanced.jlr.EnhancedAnnotatedMethodImpl;
import org.jboss.weld.annotated.slim.AnnotatedTypeIdentifier;
import org.jboss.weld.annotated.slim.SlimAnnotatedType;
import org.jboss.weld.annotated.slim.backed.BackedAnnotatedMethod;
import org.jboss.weld.injection.InjectionPointFactory;
import org.jboss.weld.injection.MethodInjectionPoint;
import org.jboss.weld.injection.MethodInjectionPoint.MethodInjectionPointType;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.serialization.spi.BeanIdentifier;
import org.jboss.weld.serialization.spi.ContextualStore;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
public class MethodInjectionPointMarshaller<T, X> implements ProtoStreamMarshaller<MethodInjectionPoint<T, X>> {

    private static final int TYPE_INDEX = 1;
    private static final int METHOD_INDEX = 2;
    private static final int BEAN_INDEX = 3;

    private static final MethodInjectionPointType DEFAULT_TYPE = MethodInjectionPointType.PRODUCER;

    static final Field TYPE_FIELD = WildFlySecurityManager.doUnchecked(new PrivilegedAction<Field>() {
        @Override
        public Field run() {
            for (Field field : MethodInjectionPoint.class.getDeclaredFields()) {
                if (field.getType() == MethodInjectionPointType.class) {
                    field.setAccessible(true);
                    return field;
                }
            }
            throw new IllegalStateException();
        }
    });

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends MethodInjectionPoint<T, X>> getJavaClass() {
        return (Class<MethodInjectionPoint<T, X>>) (Class<?>) MethodInjectionPoint.class;
    }

    @Override
    public MethodInjectionPoint<T, X> readFrom(ProtoStreamReader reader) throws IOException {
        MethodInjectionPointType injectionPointType = DEFAULT_TYPE;
        BackedAnnotatedMethod<X> method = null;
        BeanIdentifier beanId = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case TYPE_INDEX:
                    injectionPointType = reader.readEnum(MethodInjectionPointType.class);
                case METHOD_INDEX:
                    method = reader.readObject(BackedAnnotatedMethod.class);
                    break;
                case BEAN_INDEX:
                    beanId = reader.readAny(BeanIdentifier.class);
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        SlimAnnotatedType<X> type = method.getDeclaringType();
        AnnotatedTypeIdentifier identifier = type.getIdentifier();
        BeanManagerImpl manager = Container.instance(identifier).getBeanManager(identifier.getBdaId());
        ClassTransformer transformer = ClassTransformer.instance(manager);
        Bean<X> bean = (beanId != null) ? Container.instance(manager).services().get(ContextualStore.class).<Bean<X>, X>getContextual(beanId) : null;
        return InjectionPointFactory.silentInstance().createMethodInjectionPoint(injectionPointType, EnhancedAnnotatedMethodImpl.of(method, transformer.getEnhancedAnnotatedType(type), transformer), bean, getJavaClass(), Collections.emptySet(), manager);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, MethodInjectionPoint<T, X> injectionPoint) throws IOException {
        MethodInjectionPointType type = this.getValue(injectionPoint, TYPE_FIELD, MethodInjectionPointType.class);
        if (type != DEFAULT_TYPE) {
            writer.writeEnum(TYPE_INDEX, type);
        }
        BackedAnnotatedMethod<X> method = (BackedAnnotatedMethod<X>) injectionPoint.getAnnotated();
        if (method != null) {
            writer.writeObject(METHOD_INDEX, method);
        }
        Bean<?> bean = injectionPoint.getBean();
        if (bean != null) {
            BeanIdentifier beanId = Container.instance(method.getDeclaringType().getIdentifier()).services().get(ContextualStore.class).putIfAbsent(bean);
            writer.writeAny(BEAN_INDEX, beanId);
        }
    }

    private <F> F getValue(MethodInjectionPoint<T, X> injectionPoint, Field field, Class<F> targetClass) {
        return WildFlySecurityManager.doUnchecked(new PrivilegedAction<F>() {
            @Override
            public F run() {
                try {
                    return targetClass.cast(TYPE_FIELD.get(injectionPoint));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }
}
