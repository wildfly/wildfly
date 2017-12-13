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
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author bmaxwell
 *
 */
@XmlRootElement(name="servlet-info")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServletInfo implements Serializable {

    @XmlAttribute(name="servlet")
    private String servlet;
    @XmlAttribute(name="servlet-package")
    private String servletPackage;
    @XmlAttribute(name="servlet-security-role")
    private String securityRole;
    private transient Package[] packagesRequired = new Package[0];

    public ServletInfo() {
    }

    public ServletInfo(String servlet, String securityRole, Package ...packagesRequired) {
        this.servlet = servlet;
        this.servletPackage = servlet.substring(0, servlet.lastIndexOf("."));
        this.securityRole = securityRole;
        this.packagesRequired = packagesRequired;
    }

    public String getServlet() {
        return servlet;
    }

    public String getServletSimpleName() {
        return servlet.substring(servlet.lastIndexOf(".")+1, servlet.length());
    }

    public Package[] getPackagesRequired() {
        return packagesRequired;
    }

    public String getServletPackage() {
        return servletPackage;
    }
    public String getSecurityRole() {
        return securityRole;
    }
}