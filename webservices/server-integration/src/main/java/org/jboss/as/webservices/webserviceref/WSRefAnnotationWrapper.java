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

package org.jboss.as.webservices.webserviceref;

import javax.xml.ws.WebServiceRef;

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