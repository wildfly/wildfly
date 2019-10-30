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
package org.jboss.as.ee.component.deployers;

import javax.interceptor.Interceptors;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.Type;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * @author Stuart Douglas
 */
public class InterceptorsAnnotationInformationFactory extends ClassAnnotationInformationFactory<Interceptors, String[]> {

    protected InterceptorsAnnotationInformationFactory() {
        super(Interceptors.class, null);
    }

    @Override
    protected String[] fromAnnotation(final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {
        final Type[] classes =  annotationInstance.value().asClassArray();
        final String[] ret = new String[classes.length];
        for(int i = 0; i < classes.length; ++i) {
            ret[i] = classes[i].name().toString();
        }
        return ret;
    }
}
