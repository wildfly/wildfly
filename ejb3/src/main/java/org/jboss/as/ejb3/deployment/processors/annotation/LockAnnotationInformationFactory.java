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
package org.jboss.as.ejb3.deployment.processors.annotation;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.metadata.property.PropertyReplacer;

import javax.ejb.Lock;
import javax.ejb.LockType;

/**
 * Processes the {@link javax.ejb.Lock} annotation on a session bean, which allows concurrent access (like @Singleton and @Stateful beans),
 * and its methods and updates the {@link org.jboss.as.ejb3.component.session.SessionBeanComponentDescription} accordingly.
 *
 * @author Stuart Douglas
 */
public class LockAnnotationInformationFactory extends ClassAnnotationInformationFactory<Lock, LockType> {

    protected LockAnnotationInformationFactory() {
        super(Lock.class, null);
    }

    @Override
    protected LockType fromAnnotation(final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {
        AnnotationValue value = annotationInstance.value();
        if(value == null) {
            return LockType.WRITE;
        }
        return LockType.valueOf(annotationInstance.value().asEnum());
    }
}
