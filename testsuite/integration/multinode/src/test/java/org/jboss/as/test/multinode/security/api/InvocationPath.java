/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.multinode.security.api;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * @author bmaxwell
 *
 */
@XmlRootElement(name="invocation-path")
@XmlAccessorType(XmlAccessType.FIELD)
public class InvocationPath implements Serializable {

    @XmlElement(name="ejb-info")
    private EJBInfo ejbInfo;

    @XmlElement(name="servlet-info")
    private ServletInfo servletInfo;

    @XmlAttribute(name="node-name")
    private String nodeName;

    @XmlAttribute(name="caller-principal")
    private String callerPrincipal;

    @XmlElement(name="info")
    private String info;

    @XmlJavaTypeAdapter(ThrowableAdapter.class)
    @XmlElement(name="exception")
    private Throwable exception;

    public InvocationPath() {
    }

    public InvocationPath(String nodeName, String callerPrincipal) {
        this.nodeName = nodeName;
        this.callerPrincipal = callerPrincipal;
    }

    public InvocationPath(EJBInfo ejbInfo, String nodeName, String callerPrincipal) {
        this.ejbInfo = ejbInfo;
        this.nodeName = nodeName;
        this.callerPrincipal = callerPrincipal;
    }
    public InvocationPath(ServletInfo servletInfo, String nodeName, String callerPrincipal) {
        this.servletInfo = servletInfo;
        this.nodeName = nodeName;
        this.callerPrincipal = callerPrincipal;
    }


    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public EJBInfo getEjbInfo() {
        return ejbInfo;
    }
    public ServletInfo getServletInfo() {
        return servletInfo;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getCallerPrincipal() {
        return callerPrincipal;
    }
    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }
}