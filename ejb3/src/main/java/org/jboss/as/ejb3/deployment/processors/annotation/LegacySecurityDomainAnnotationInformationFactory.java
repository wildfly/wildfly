/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017, Red Hat Inc., and individual contributors as indicated
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
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.security.annotation.SecurityDomain;

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;
/**
 * The {@link org.jboss.security.annotation.SecurityDomain} annotation on a session bean
 * is not used anymore. Log a warning message and ignore the process.
 *
 * This class can be removed when picketbox is dropped completely from WildFly.
 * See: https://issues.jboss.org/browse/WFLY-9168
 *
 * @author Lin Gao
 * @Deprecated Please use {@link org.jboss.ejb3.annotation.SecurityDomain} instead.
 */
@Deprecated
public class LegacySecurityDomainAnnotationInformationFactory extends ClassAnnotationInformationFactory<SecurityDomain, String> {

    protected LegacySecurityDomainAnnotationInformationFactory() {
        super(SecurityDomain.class, null);
    }

    @Override
    protected String fromAnnotation(final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {
        ROOT_LOGGER.legacySecurityDomainAnnotationIsUsed(annotationInstance.target().asClass().toString());
        return null;
    }
}
