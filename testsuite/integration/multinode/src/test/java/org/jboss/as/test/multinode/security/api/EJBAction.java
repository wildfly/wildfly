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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.jboss.as.test.multinode.security.util.EJBUtil;
import org.jboss.logging.Logger;

/**
 * @author bmaxwell
 *
 */
@XmlRootElement(name="ejb-action")
@XmlAccessorType(XmlAccessType.FIELD)
public class EJBAction implements Serializable {

    @XmlTransient
    private static Logger log = Logger.getLogger(EJBAction.class.getName());

    @XmlElement(name="remote-ejb-config")
    private RemoteEJBConfig remoteEJBConfig;

    @XmlElement(name="ejb-info")
    private EJBInfo ejbInfo;

    public EJBAction() {
    }

    public EJBAction(RemoteEJBConfig remoteEJBConfig, EJBInfo ejbInfo) {
        this.remoteEJBConfig = remoteEJBConfig;
        this.ejbInfo = ejbInfo;
    }

    public EJBRequest invoke(EJBRequest ejbRequest) throws Exception {
        SecuritySLSBRemote remoteEJB = null;
        String lookup = "";
        Context ctx = null;
        if (remoteEJBConfig.isJbossEjbClientXml()) {
            ctx = new InitialContext();
            lookup = ejbInfo.getRemoteLookupPath();
        } else if (remoteEJBConfig.isRemote()) {
            log.debug("Invoking remote: " + remoteEJBConfig);
            if (remoteEJBConfig.getUsername() == null) {
                ctx = EJBUtil.getWildflyInitialContext(remoteEJBConfig.getHost(), remoteEJBConfig.getPort(), null, null);
            } else {
                ctx = EJBUtil.getWildflyInitialContext(remoteEJBConfig.getHost(), remoteEJBConfig.getPort(),
                        remoteEJBConfig.getUsername(), remoteEJBConfig.getPassword());
            }
            lookup = ejbInfo.getRemoteLookupPath();
        } else {
            log.debug("Invoking InVM remote interface: " + remoteEJBConfig);
            ctx = new InitialContext();
            lookup = ejbInfo.getInVmGlobalLookupPath();
        }
        log.debug("Lookup: " + lookup);
        remoteEJB = (SecuritySLSBRemote) ctx.lookup(lookup);
        log.debug("invoking with: " + ejbRequest);
        return remoteEJB.invoke(ejbRequest);
    }

    @Override
    public String toString() {
        return String.format("EJBAction invoke %s using %s", ejbInfo.getRemoteLookupPath(), remoteEJBConfig.toString());
    }
}