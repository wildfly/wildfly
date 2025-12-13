/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.injection;

import java.io.IOException;
import java.lang.reflect.Method;

import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.Bean;

import org.infinispan.protostream.descriptors.WireType;
import org.jboss.weld.Container;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedCallable;
import org.jboss.weld.annotated.enhanced.jlr.EnhancedAnnotatedConstructorImpl;
import org.jboss.weld.annotated.enhanced.jlr.EnhancedAnnotatedMethodImpl;
import org.jboss.weld.annotated.enhanced.jlr.EnhancedAnnotatedParameterImpl;
import org.jboss.weld.annotated.slim.AnnotatedTypeIdentifier;
import org.jboss.weld.annotated.slim.SlimAnnotatedType;
import org.jboss.weld.annotated.slim.backed.BackedAnnotatedCallable;
import org.jboss.weld.annotated.slim.backed.BackedAnnotatedParameter;
import org.jboss.weld.injection.InjectionPointFactory;
import org.jboss.weld.injection.ParameterInjectionPointImpl;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.serialization.spi.BeanIdentifier;
import org.jboss.weld.serialization.spi.ContextualStore;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public class ParameterInjectionPointMarshaller<T, X> implements ProtoStreamMarshaller<ParameterInjectionPointImpl<T, X>> {

    private static final int PARAMETER_INDEX = 1;
    private static final int BEAN_INDEX = 2;

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends ParameterInjectionPointImpl<T, X>> getJavaClass() {
        return (Class<ParameterInjectionPointImpl<T, X>>) (Class<?>) ParameterInjectionPointImpl.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ParameterInjectionPointImpl<T, X> readFrom(ProtoStreamReader reader) throws IOException {
        BackedAnnotatedParameter<X> parameter = null;
        BeanIdentifier beanId = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case PARAMETER_INDEX:
                    parameter = reader.readObject(BackedAnnotatedParameter.class);
                    break;
                case BEAN_INDEX:
                    beanId = reader.readAny(BeanIdentifier.class);
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        BackedAnnotatedCallable<X, ?> callable = parameter.getDeclaringCallable();
        SlimAnnotatedType<X> type = callable.getDeclaringType();
        AnnotatedTypeIdentifier identifier = type.getIdentifier();
        BeanManagerImpl manager = Container.instance(identifier).getBeanManager(identifier.getBdaId());
        ClassTransformer transformer = ClassTransformer.instance(manager);
        Bean<?> bean = (beanId != null) ? Container.instance(manager).services().get(ContextualStore.class).<Bean<Object>, Object>getContextual(beanId) : null;
        EnhancedAnnotatedCallable<?, X, ?> enhancedCallable = (callable.getJavaMember() instanceof Method) ? EnhancedAnnotatedMethodImpl.of((AnnotatedMethod<X>) callable, transformer.getEnhancedAnnotatedType(type), transformer) : EnhancedAnnotatedConstructorImpl.of((AnnotatedConstructor<X>) callable, transformer.getEnhancedAnnotatedType(type), transformer);
        return (ParameterInjectionPointImpl<T, X>) InjectionPointFactory.silentInstance().createParameterInjectionPoint(EnhancedAnnotatedParameterImpl.<T, X>of(parameter, enhancedCallable, transformer), bean, getJavaClass(), manager);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, ParameterInjectionPointImpl<T, X> injectionPoint) throws IOException {
        BackedAnnotatedParameter<X> parameter = (BackedAnnotatedParameter<X>) injectionPoint.getAnnotated();
        if (parameter != null) {
            writer.writeObject(PARAMETER_INDEX, parameter);
        }
        Bean<?> bean = injectionPoint.getBean();
        if (bean != null) {
            BeanIdentifier beanId = Container.instance(parameter.getDeclaringCallable().getDeclaringType().getIdentifier()).services().get(ContextualStore.class).putIfAbsent(bean);
            writer.writeAny(BEAN_INDEX, beanId);
        }
    }
}
