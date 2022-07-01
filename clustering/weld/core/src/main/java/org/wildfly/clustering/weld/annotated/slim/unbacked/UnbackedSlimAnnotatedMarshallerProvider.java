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

package org.wildfly.clustering.weld.annotated.slim.unbacked;

import org.jboss.weld.annotated.slim.unbacked.UnbackedAnnotatedConstructor;
import org.jboss.weld.annotated.slim.unbacked.UnbackedAnnotatedField;
import org.jboss.weld.annotated.slim.unbacked.UnbackedAnnotatedMethod;
import org.jboss.weld.annotated.slim.unbacked.UnbackedAnnotatedParameter;
import org.jboss.weld.annotated.slim.unbacked.UnbackedAnnotatedType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedConstructorMarshaller;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedFieldMarshaller;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedMethodMarshaller;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedParameterMarshaller;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedTypeMarshaller;

/**
 * @author Paul Ferraro
 */
public enum UnbackedSlimAnnotatedMarshallerProvider implements ProtoStreamMarshallerProvider {

    CONSTRUCTOR(new AnnotatedConstructorMarshaller<>(UnbackedAnnotatedConstructor.class, UnbackedAnnotatedType.class)),
    FIELD(new AnnotatedFieldMarshaller<>(UnbackedAnnotatedField.class, UnbackedAnnotatedType.class)),
    METHOD(new AnnotatedMethodMarshaller<>(UnbackedAnnotatedMethod.class, UnbackedAnnotatedType.class)),
    PARAMETER(new AnnotatedParameterMarshaller<>(UnbackedAnnotatedParameter.class, UnbackedAnnotatedConstructor.class, UnbackedAnnotatedMethod.class)),
    TYPE(new AnnotatedTypeMarshaller<>(UnbackedAnnotatedType.class)),
    IDENTIFIER(new UnbackedMemberIdentifierMarshaller<>()),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    UnbackedSlimAnnotatedMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
