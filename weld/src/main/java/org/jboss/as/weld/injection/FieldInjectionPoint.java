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

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * InjectionPoint implementation for a field
 */
final class FieldInjectionPoint implements InjectionPoint {

    private final AnnotatedField<?> field;
    private final Set<Annotation> qualifiers;
    private final Bean<?> bean;

    public FieldInjectionPoint(AnnotatedField<?> field, Set<Annotation> qualifiers, Bean<?> bean) {
        this.field = field;
        this.qualifiers = qualifiers;
        this.bean = bean;
    }

    @Override
    public Type getType() {
        return field.getJavaMember().getGenericType();

    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    @Override
    public Bean<?> getBean() {
        return bean;
    }

    @Override
    public Member getMember() {
        return field.getJavaMember();
    }

    @Override
    public Annotated getAnnotated() {
        return field;
    }

    @Override
    public boolean isDelegate() {
        return false;
    }

    @Override
    public boolean isTransient() {
        return Modifier.isTransient(field.getJavaMember().getModifiers());
    }
}
