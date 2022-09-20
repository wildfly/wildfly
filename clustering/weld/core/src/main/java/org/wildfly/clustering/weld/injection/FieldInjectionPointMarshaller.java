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

package org.wildfly.clustering.weld.injection;

import java.io.IOException;

import jakarta.enterprise.inject.spi.Bean;

import org.infinispan.protostream.descriptors.WireType;
import org.jboss.weld.Container;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedField;
import org.jboss.weld.annotated.enhanced.jlr.EnhancedAnnotatedFieldImpl;
import org.jboss.weld.annotated.slim.AnnotatedTypeIdentifier;
import org.jboss.weld.annotated.slim.SlimAnnotatedType;
import org.jboss.weld.annotated.slim.backed.BackedAnnotatedField;
import org.jboss.weld.injection.FieldInjectionPoint;
import org.jboss.weld.injection.InjectionPointFactory;
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
public class FieldInjectionPointMarshaller<T, X> implements ProtoStreamMarshaller<FieldInjectionPoint<T, X>> {

    private static final int FIELD_INDEX = 1;
    private static final int BEAN_INDEX = 2;

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends FieldInjectionPoint<T, X>> getJavaClass() {
        return (Class<FieldInjectionPoint<T, X>>) (Class<?>) FieldInjectionPoint.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public FieldInjectionPoint<T, X> readFrom(ProtoStreamReader reader) throws IOException {
        BackedAnnotatedField<X> field = null;
        BeanIdentifier beanId = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case FIELD_INDEX:
                    field = reader.readObject(BackedAnnotatedField.class);
                    break;
                case BEAN_INDEX:
                    beanId = reader.readAny(BeanIdentifier.class);
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        SlimAnnotatedType<X> type = field.getDeclaringType();
        AnnotatedTypeIdentifier identifier = type.getIdentifier();
        BeanManagerImpl manager = Container.instance(identifier).getBeanManager(identifier.getBdaId());
        ClassTransformer transformer = ClassTransformer.instance(manager);
        Bean<?> bean = (beanId != null) ? Container.instance(manager).services().get(ContextualStore.class).<Bean<Object>, Object>getContextual(beanId) : null;
        EnhancedAnnotatedField<T, X> enhancedField = (EnhancedAnnotatedField<T, X>) EnhancedAnnotatedFieldImpl.of(field, transformer.getEnhancedAnnotatedType(type), transformer);
        return InjectionPointFactory.silentInstance().createFieldInjectionPoint(enhancedField, bean, type.getJavaClass(), manager);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, FieldInjectionPoint<T, X> injectionPoint) throws IOException {
        BackedAnnotatedField<X> field = (BackedAnnotatedField<X>) injectionPoint.getAnnotated();
        if (field != null) {
            writer.writeObject(FIELD_INDEX, field);
        }
        Bean<?> bean = injectionPoint.getBean();
        if (bean != null) {
            BeanIdentifier beanId = Container.instance(field.getDeclaringType().getIdentifier()).services().get(ContextualStore.class).putIfAbsent(bean);
            writer.writeAny(BEAN_INDEX, beanId);
        }
    }
}
