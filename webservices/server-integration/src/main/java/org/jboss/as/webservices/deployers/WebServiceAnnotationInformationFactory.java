/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.deployers;

import jakarta.jws.WebService;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
public class WebServiceAnnotationInformationFactory extends
        ClassAnnotationInformationFactory<WebService, WebServiceAnnotationInfo> {

    protected WebServiceAnnotationInformationFactory() {
        super(jakarta.jws.WebService.class, null);
    }

    @Override
    protected WebServiceAnnotationInfo fromAnnotation(final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {
        String nameValue = asString(annotationInstance, "name");
        String targetNamespacValue = asString(annotationInstance, "targetNamespace");
        String serviceNameValue = asString(annotationInstance, "serviceName");
        String portNameValue = asString(annotationInstance, "portName");
        String wsdlLocationValue = asString(annotationInstance, "wsdlLocation");
        String endpointInterfaceValue = asString(annotationInstance, "endpointInterface");
        return new WebServiceAnnotationInfo(endpointInterfaceValue, nameValue, portNameValue, serviceNameValue,
                targetNamespacValue, wsdlLocationValue, annotationInstance.target());
    }

    private String asString(final AnnotationInstance annotation, String property) {
        AnnotationValue value = annotation.value(property);
        return value == null ? "" : value.asString();
    }
}
