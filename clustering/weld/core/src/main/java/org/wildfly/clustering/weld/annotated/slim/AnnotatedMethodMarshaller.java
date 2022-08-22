/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.weld.annotated.slim;

import java.lang.reflect.Method;

import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;

import org.jboss.weld.annotated.slim.SlimAnnotatedType;
import org.jboss.weld.util.reflection.DeclaredMemberIndexer;

/**
 * Generic marshaller for an {@link AnnotatedMethod}.
 * @author Paul Ferraro
 */
public class AnnotatedMethodMarshaller<X, T extends SlimAnnotatedType<X>, M extends AnnotatedMethod<X>> extends AnnotatedCallableMarshaller<X, Method, T, AnnotatedMethod<? super X>, M> {

    public AnnotatedMethodMarshaller(Class<M> targetClass, Class<T> typeClass) {
        super(targetClass, typeClass, DeclaredMemberIndexer::getMethodForIndex, AnnotatedType::getMethods, DeclaredMemberIndexer::getIndexForMethod);
    }
}
