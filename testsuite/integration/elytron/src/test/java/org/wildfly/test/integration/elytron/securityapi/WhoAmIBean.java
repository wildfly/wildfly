/*
 * Copyright 2018 Red Hat, Inc.
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

package org.wildfly.test.integration.elytron.securityapi;

import java.security.Principal;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.security.enterprise.SecurityContext;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * Concrete implementation to allow deployment of bean.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@Stateless
@SecurityDomain("other")
public class WhoAmIBean implements WhoAmI {

    @Resource
    private SessionContext sessionContext;

    @Inject
    private SecurityContext securityContext;

    @Override
    public Principal getCallerPrincipalSessionContext() {
        return sessionContext.getCallerPrincipal();
    }

    @Override
    public Principal getCallerPrincipalSecurityDomain() {
        return org.wildfly.security.auth.server.SecurityDomain.getCurrent().getCurrentSecurityIdentity().getPrincipal();
    }

    @Override
    public Principal getCallerPrincipalSecurityContext() {
        return securityContext.getCallerPrincipal();
    }
}
