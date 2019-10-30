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
package org.wildfly.test.manual.elytron.seccontext;

import java.security.Principal;
import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

/**
 * Stateless implementation of the {@link WhoAmI}.
 * @author Josef Cacek
 */
@Stateless
@RolesAllowed({ "whoami", "admin", "no-server2-identity", "authz" })
@DeclareRoles({ "entry", "whoami", "servlet", "admin", "no-server2-identity", "authz" })
public class WhoAmIBean implements WhoAmI {

    @Resource
    private SessionContext context;

    @Override
    public Principal getCallerPrincipal() {
        return context.getCallerPrincipal();
    }

    @Override
    public String throwIllegalStateException() {
        throw new IllegalStateException("Expected IllegalStateException from WhoAmIBean.");
    }

    @Override
    public String throwServer2Exception() {
        throw new Server2Exception("Expected Server2Exception from WhoAmIBean.");
    }

}
