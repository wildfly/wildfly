/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.deployment.processors.annotation;


import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.ejb3.annotation.Pool;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * Processes {@link Pool} annotation on EJB classes
 *
 * @author Jaikiran Pai
 */
public class PoolAnnotationInformationFactory extends ClassAnnotationInformationFactory<Pool, String> {

    public PoolAnnotationInformationFactory() {
        super(Pool.class, null);
    }

    @Override
    protected String fromAnnotation(final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {
        AnnotationValue value = annotationInstance.value();
        if (value == null || value.asString().isEmpty()) {
            throw EeLogger.ROOT_LOGGER.annotationAttributeMissing("@Pool", "value");
        }
        return propertyReplacer.replaceProperties(value.asString());
    }
}
