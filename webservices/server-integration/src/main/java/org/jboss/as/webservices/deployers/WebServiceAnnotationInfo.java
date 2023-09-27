/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.deployers;

import org.jboss.jandex.AnnotationTarget;

/**
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
public class WebServiceAnnotationInfo {
    private final String wsdlLocation;

    private final String endpointInterface;

    private final String portName;

    private final String serviceName;

    private final String name;

    private final String targetNamespace;

    private final AnnotationTarget target;

    public WebServiceAnnotationInfo(final String endpointInterface, final String name, final String portName, final String servicename, final String targetNamespace, final String wsdlLocation, final AnnotationTarget target) {
        this.wsdlLocation = wsdlLocation;
        this.endpointInterface = endpointInterface;
        this.portName = portName;
        this.serviceName = servicename;
        this.name = name;
        this.targetNamespace = targetNamespace;
        this.target = target;
    }

    public String getTargetNamespace() {
        return targetNamespace;
    }

    public String getName() {
        return name;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getPortName() {
        return portName;
    }

    public String getEndpointInterface() {
        return endpointInterface;
    }

    public String getWsdlLocation() {
        return wsdlLocation;
    }


    public AnnotationTarget getTarget() {
        return target;
    }
}
