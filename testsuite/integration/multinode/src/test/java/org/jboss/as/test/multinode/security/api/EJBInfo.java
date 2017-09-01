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
@XmlRootElement(name="ejb-info")
@XmlAccessorType(XmlAccessType.FIELD)
public class EJBInfo implements Serializable {

    @XmlAttribute(name="application-name")
    private String applicationName;

    @XmlAttribute(name="module-name")
    private String moduleName;

    @XmlAttribute(name="ejb-name")
    private String ejbName;

    @XmlAttribute(name="ejb-package")
    private String ejbPackage;

    @XmlAttribute(name="ejb-interface")
    private Class ejbInterface;

    public EJBInfo() {
    }

    public EJBInfo(String applicationName, String moduleName, String ejbName, String ejbPackage, Class ejbInterface) {
        if(applicationName == null)
            applicationName = "";
        if(moduleName == null)
            moduleName = "";
        this.applicationName = applicationName;
        this.moduleName = moduleName;
        this.ejbName = ejbName;
        this.ejbPackage = ejbPackage;
        this.ejbInterface = ejbInterface;
    }

    public String getEjbPackage() {
        return ejbPackage;
    }
    public String getApplicationName() {
        return applicationName;
    }
    public String getModuleName() {
        return moduleName;
    }
    public String getEjbName() {
        return ejbName;
    }
    public Class getEjbInterface() {
        return ejbInterface;
    }
    public String getRemoteLookupPath() {
        return String.format("ejb:%s/%s/%s!%s", applicationName, moduleName, ejbName, ejbInterface.getName());
    }
    public String getInVmGlobalLookupPath() {
        return String.format("java:global:/%s/%s/%s!%s", applicationName, moduleName, ejbName, ejbInterface.getName());
    }
}