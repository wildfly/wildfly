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
