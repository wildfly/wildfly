/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.SerializeFunctionWith;
import org.infinispan.commons.marshall.SerializeWith;
import org.jboss.marshalling.AnnotationClassExternalizerFactory;
import org.jboss.marshalling.ClassExternalizerFactory;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A JBM2-compatible version of {@link org.infinispan.commons.marshall.jboss.SerializeWithExtFactory}.
 * @author Paul Ferraro
 */
public class SerializeWithClassExternalizerFactory implements ClassExternalizerFactory {

    private final ClassExternalizerFactory defaultFactory = new AnnotationClassExternalizerFactory();

    @Override
    public org.jboss.marshalling.Externalizer getExternalizer(Class<?> type) {
        SerializeWith annotation = type.getAnnotation(SerializeWith.class);
        Class<? extends Externalizer<?>> externalizerClass = (annotation != null) ? annotation.value() : null;
        if (externalizerClass == null) {
            SerializeFunctionWith lambdaAnnotation = type.getAnnotation(SerializeFunctionWith.class);
            if (lambdaAnnotation != null) {
                externalizerClass = lambdaAnnotation.value();
            }
        }
        return (externalizerClass != null) ? new ExternalizerAdapter(newInstance(externalizerClass)) : this.defaultFactory.getExternalizer(type);
    }

    private static <T> T newInstance(Class<? extends T> targetClass) {
        try {
            PrivilegedExceptionAction<T> constructor = () -> targetClass.newInstance();
            return WildFlySecurityManager.doUnchecked(constructor);
        } catch (PrivilegedActionException e) {
            throw new IllegalArgumentException(targetClass.getName());
        }
    }
}
