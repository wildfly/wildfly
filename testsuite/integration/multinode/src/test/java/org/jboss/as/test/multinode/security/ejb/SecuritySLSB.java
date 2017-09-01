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

package org.jboss.as.test.multinode.security.ejb;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

import org.jboss.as.test.multinode.security.api.EJBAction;
import org.jboss.as.test.multinode.security.api.EJBRequest;
import org.jboss.as.test.multinode.security.api.InvocationPath;
import org.jboss.as.test.multinode.security.api.SecuritySLSBLocal;
import org.jboss.as.test.multinode.security.api.SecuritySLSBRemote;
import org.jboss.as.test.multinode.security.api.TestConfig;
import org.jboss.ejb3.annotation.SecurityDomain;
import org.jboss.logging.Logger;

/**
 * @author bmaxwell
 *
 */
@Stateless
@SecurityDomain(TestConfig.SECURITY_DOMAIN)
@RolesAllowed({TestConfig.SECURITY_EJB_ROLE})
public class SecuritySLSB implements SecuritySLSBLocal, SecuritySLSBRemote {

    private Logger log = Logger.getLogger(this.getClass().getName());
    private String nodeName = System.getProperty("jboss.node.name");

    @Resource
    private SessionContext context;

    @Override
    public EJBRequest invoke(EJBRequest request) {
        EJBRequest response = request;
        String caller = context.getCallerPrincipal() == null ? "null" : context.getCallerPrincipal().toString();
        log.debug("Caller:" + caller);
        InvocationPath path = new InvocationPath(TestConfig.SECURITY_EJB1, nodeName, caller);
        response.getInvocationPath().add(path);

        // call other ejbs
        EJBAction action = null;
        try {
            boolean hasAction = (response.getActions().isEmpty() == false);
            while(hasAction) {
                action = response.getActions().remove(0);
                log.debug("EJBAction: " + action.toString());
                response = action.invoke(response);
                hasAction = (response.getActions().isEmpty() == false);
            }
        } catch(Exception e) {
            path.setException(new Exception(action.toString(), e));
        }
        return response;
    }
}