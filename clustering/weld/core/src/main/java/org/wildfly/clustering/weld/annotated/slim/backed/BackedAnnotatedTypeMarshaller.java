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

import org.jboss.weld.annotated.slim.AnnotatedTypeIdentifier;
import org.jboss.weld.annotated.slim.backed.BackedAnnotatedType;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedTypeMarshaller;

/**
 * Validates marshalling of {@link BackedAnnotatedType}.
 * @author Paul Ferraro
 */
public class BackedAnnotatedTypeMarshaller<X> extends AnnotatedTypeMarshaller<X, BackedAnnotatedType<X>> {

    @SuppressWarnings("unchecked")
    public BackedAnnotatedTypeMarshaller() {
        super((Class<BackedAnnotatedType<X>>) (Class<?>) BackedAnnotatedType.class);
    }

    @Override
    protected BackedAnnotatedType<X> getAnnotatedType(AnnotatedTypeIdentifier identifier, BeanManagerImpl manager) {
        BackedAnnotatedType<X> result = super.getAnnotatedType(identifier, manager);
        // If type is not yet know, attempt to load it
        if (result == null) {
            @SuppressWarnings("unchecked")
            Class<X> targetClass = (Class<X>) manager.getServices().get(ResourceLoader.class).classForName(identifier.getClassName());
            result = manager.getServices().get(ClassTransformer.class).getBackedAnnotatedType(targetClass, identifier.getBdaId());
        }
        return result;
    }
}
