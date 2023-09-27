/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.xts.jandex;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.xts.XTSException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import jakarta.jws.WebService;
import javax.xml.namespace.QName;

/**
 * @author paul.robinson@redhat.com, 2012-02-06
 */
public class WebServiceAnnotation {

    private static final String WEBSERVICE_ANNOTATION = WebService.class.getName();

    private final String portName;

    private final String serviceName;

    private final String name;

    private final String targetNamespace;

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

        final String portName = getStringValue(annotationInstance, "portName");
        final String serviceName = getStringValue(annotationInstance, "serviceName");
        final String name = getStringValue(annotationInstance, "name");
        final String targetNamespace = getStringValue(annotationInstance, "targetNamespace");

        return new WebServiceAnnotation(portName, serviceName, name, targetNamespace);
    }

    private static String getStringValue(AnnotationInstance annotationInstance, String key) {
        final AnnotationValue value = annotationInstance.value(key);

        if (value != null) {
            return value.asString();
        } else if ("portName".equals(key)) {
            return getTargetClassName(annotationInstance) + "Port";
        } else if ("serviceName".equals(key)) {
            return getTargetClassName(annotationInstance) + "Service";
        } else if ("name".equals(key)) {
            return getTargetClassName(annotationInstance);
        } else if ("targetNamespace".equals(key)) {
            return getTargetNamespace(annotationInstance);
        } else {
            return "";
        }
    }

    private static String getTargetClassName(final AnnotationInstance annotationInstance) {
        final String fullName = annotationInstance.target().toString();
        final int lastDotIndex = fullName.lastIndexOf(".");

        if (lastDotIndex == -1) {
            return fullName;
        } else {
            return fullName.substring(lastDotIndex + 1);
        }
    }

    private static String getTargetNamespace(final AnnotationInstance annotationInstance) {
        final String[] parts = annotationInstance.target().toString().split("\\.");

        if (parts.length < 2) {
            return "";
        }

        String targetNamespace = "http://";

        for (int i = parts.length - 2; i >= 0; i--) {
            targetNamespace += parts[i] + ".";
        }

        return targetNamespace.substring(0, targetNamespace.length() - 1) + "/";
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
