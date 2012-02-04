/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.xts.jandex;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.xts.XTSException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import javax.xml.namespace.QName;

/**
 * @author paul.robinson@redhat.com, 2012-02-06
 */
public class WebServiceAnnotation {

    private static final String WEBSERVICE_ANNOTATION = "javax.jws.WebService";

    private String portName;

    private String serviceName;

    private String name;

    private String targetNamespace;

    private WebServiceAnnotation(String portName, String serviceName, String name, String targetNamespace) {
        this.portName = portName;
        this.serviceName = serviceName;
        this.name = name;
        this.targetNamespace = targetNamespace;
    }

    public static WebServiceAnnotation build(DeploymentUnit unit, String endpoint) throws XTSException {
        AnnotationInstance annotationInstance = JandexHelper.getAnnotation(unit, endpoint, WEBSERVICE_ANNOTATION);

        if (annotationInstance == null) {
            return null;
        }

        final String portName = getStringVaue(annotationInstance, "portName");
        final String serviceName = getStringVaue(annotationInstance, "serviceName");
        final String name = getStringVaue(annotationInstance, "name");
        final String targetNamespace = getStringVaue(annotationInstance, "targetNamespace");

        return new WebServiceAnnotation(portName, serviceName, name, targetNamespace);
    }

    private static String getStringVaue(AnnotationInstance annotationInstance, String key) {
        AnnotationValue value = annotationInstance.value(key);
        if (value == null) {
            return null;
        }
        return value.asString();
    }

    public QName buildPortQName() {
        return new QName(targetNamespace, portName);
    }

    public String getPortName() {
        return portName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getName() {
        return name;
    }

    public String getTargetNamespace() {
        return targetNamespace;
    }
}