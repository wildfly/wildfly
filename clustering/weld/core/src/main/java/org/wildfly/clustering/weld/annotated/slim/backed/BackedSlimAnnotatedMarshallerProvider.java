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

package org.wildfly.clustering.weld.annotated.slim.backed;

import org.jboss.weld.annotated.slim.backed.BackedAnnotatedConstructor;
import org.jboss.weld.annotated.slim.backed.BackedAnnotatedField;
import org.jboss.weld.annotated.slim.backed.BackedAnnotatedMethod;
import org.jboss.weld.annotated.slim.backed.BackedAnnotatedParameter;
import org.jboss.weld.annotated.slim.backed.BackedAnnotatedType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedConstructorMarshaller;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedFieldMarshaller;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedMethodMarshaller;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedParameterMarshaller;

/**
 * @author Paul Ferraro
 */
public enum BackedSlimAnnotatedMarshallerProvider implements ProtoStreamMarshallerProvider {

    CONSTRUCTOR(new AnnotatedConstructorMarshaller<>(BackedAnnotatedConstructor.class, BackedAnnotatedType.class)),
    FIELD(new AnnotatedFieldMarshaller<>(BackedAnnotatedField.class, BackedAnnotatedType.class)),
    METHOD(new AnnotatedMethodMarshaller<>(BackedAnnotatedMethod.class, BackedAnnotatedType.class)),
    PARAMETER(new AnnotatedParameterMarshaller<>(BackedAnnotatedParameter.class, BackedAnnotatedConstructor.class, BackedAnnotatedMethod.class)),
    TYPE(new BackedAnnotatedTypeMarshaller<>()),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    BackedSlimAnnotatedMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
