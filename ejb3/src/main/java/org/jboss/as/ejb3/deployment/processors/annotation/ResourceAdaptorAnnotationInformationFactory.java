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
import org.jboss.as.ejb3.util.PropertiesValueResolver;
import org.jboss.jandex.AnnotationInstance;

/**
 * Processes the {@link org.jboss.ejb3.annotation.ResourceAdapter} annotation
 *
 * @author Stuart Douglas
 */
public class ResourceAdaptorAnnotationInformationFactory extends ClassAnnotationInformationFactory<org.jboss.ejb3.annotation.ResourceAdapter, String> {

    protected ResourceAdaptorAnnotationInformationFactory() {
        super(org.jboss.ejb3.annotation.ResourceAdapter.class, null);
    }

    @Override
    protected String fromAnnotation(final AnnotationInstance annotationInstance, final boolean replacement) {
        String resourceAdapterValue = annotationInstance.value().asString();
        if(replacement)
            return PropertiesValueResolver.replaceProperties(resourceAdapterValue);
        else
            return resourceAdapterValue;
    }
}
