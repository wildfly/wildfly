/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.webservices.deployers;

import javax.xml.ws.WebServiceProvider;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
public class WebServiceProviderAnnotationInformationFactory extends
        ClassAnnotationInformationFactory<WebServiceProvider, WebServiceProviderAnnotationInfo> {

    protected WebServiceProviderAnnotationInformationFactory() {
        super(WebServiceProvider.class, null);
    }

    @Override
    protected WebServiceProviderAnnotationInfo fromAnnotation(final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {
        String targetNamespacValue = asString(annotationInstance, "targetNamespace");
        String serviceNameValue = asString(annotationInstance, "serviceName");
        String portNameValue = asString(annotationInstance, "portName");
        String wsdlLocationValue = asString(annotationInstance, "wsdlLocation");
        return new WebServiceProviderAnnotationInfo(portNameValue, serviceNameValue, targetNamespacValue, wsdlLocationValue,
                annotationInstance.target());
    }

    private String asString(final AnnotationInstance annotation, String property) {
        AnnotationValue value = annotation.value(property);
        return value == null ? "" : value.asString();
    }
}
