/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.deployers;

import org.jboss.jandex.AnnotationTarget;

/**
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
public class WebServiceProviderAnnotationInfo {

    private final String wsdlLocation;

    private final String portName;

    private final String serviceName;

    private final String targetNamespace;

    private final AnnotationTarget target;


    public WebServiceProviderAnnotationInfo(final String portName, final String serviceName, final String targetNamespace, final String wsdlLocation, final AnnotationTarget target) {
        this.wsdlLocation = wsdlLocation;
        this.portName = portName;
        this.serviceName = serviceName;
        this.targetNamespace = targetNamespace;
        this.target = target;
    }

    public String getTargetNamespace() {
        return targetNamespace;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getPortName() {
        return portName;
    }

    public String getWsdlLocation() {
        return wsdlLocation;
    }

    public AnnotationTarget getTarget() {
        return target;
    }


}
