/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.webservices.webserviceref;

import jakarta.xml.ws.WebServiceRef;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

/**
 * WebServiceRef annotation wrapper.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WSRefAnnotationWrapper {
    private final String type;
    private final String name;
    private final String value;
    private final String wsdlLocation;

    public WSRefAnnotationWrapper(final AnnotationInstance annotation) {
        name = stringValueOrNull(annotation, "name");
        type = classValueOrNull(annotation, "type");
        value = classValueOrNull(annotation, "value");
        wsdlLocation = stringValueOrNull(annotation, "wsdlLocation");
    }

    public WSRefAnnotationWrapper(final WebServiceRef annotation) {
        name = annotation.name().isEmpty() ? null : annotation.name();
        type = annotation.type() == Object.class ? null : annotation.type().getName();
        value = annotation.value().getName();
        wsdlLocation = annotation.wsdlLocation().isEmpty() ? null : annotation.wsdlLocation();
    }


    public String name() {
        return name;
    }

    public String type() {
        return type;
    }

    public String value() {
        return value;
    }

    public String wsdlLocation() {
        return wsdlLocation;
    }

    private String stringValueOrNull(final AnnotationInstance annotation, final String attribute) {
        final AnnotationValue value = annotation.value(attribute);
        return value != null ? value.asString() : null;
    }

    private String classValueOrNull(final AnnotationInstance annotation, final String attribute) {
        final AnnotationValue value = annotation.value(attribute);
        return value != null ? value.asClass().name().toString() : null;
    }
}
