/*
 * Copyright 2021 Red Hat, Inc.
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
package org.jboss.as.test.integration.ws.wsse;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.jws.WebService;
import org.apache.cxf.annotations.EndpointProperties;
import org.apache.cxf.annotations.EndpointProperty;
import org.apache.cxf.interceptor.InInterceptors;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;

/**
 * <p>Implementation for the ServiceIface that obtains the current elytron
 * security identity and checks that it is correctly filled (roles and
 * attributes are not empty). It is only allowed for users with role
 * <em>Users</em>.</p>
 *
 * @author rmartinc
 */
@Stateless
@RolesAllowed("Users")
@WebService(
        portName = "UsernameTokenPort",
        serviceName = "UsernameToken",
        wsdlLocation = "WEB-INF/wsdl/UsernameToken.wsdl",
        targetNamespace = "http://www.jboss.org/jbossws/ws-extensions/wssecuritypolicy",
        endpointInterface = "org.jboss.as.test.integration.ws.wsse.ServiceIface"
)
@EndpointProperties(value = {
    @EndpointProperty(key = "ws-security.validate.token", value = "false")
})
@InInterceptors(interceptors = {
    "org.jboss.wsf.stack.cxf.security.authentication.SubjectCreatingPolicyInterceptor"
})
public class ElytronUsernameTokenImpl implements ServiceIface {

    @Override
    public String sayHello() {
        String username = "World";
        boolean roles = false, attributes = false;
        SecurityDomain sd = SecurityDomain.getCurrent();
        if (sd != null && sd.getCurrentSecurityIdentity() != null) {
            SecurityIdentity si = sd.getCurrentSecurityIdentity();
            username = si.getPrincipal().getName();
            roles = !si.getRoles().isEmpty();
            attributes = !si.getAttributes().isEmpty();
        }

        return new StringBuilder("Hello ")
                .append(username)
                .append(roles? " with roles" : " without roles")
                .append(attributes? " and with attributes" : " and without attributes")
                .append("!")
                .toString();
    }
}
