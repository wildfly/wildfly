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

import jakarta.ejb.Local;

/**
 * The local interface to the simple WhoAmI bean.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@Local
public interface WhoAmI {

    /**
     * @return the caller principal obtained from the SessionContext.
     */
    Principal getCallerPrincipalSessionContext();

    /**
     * @return the caller principal obtained from the SecurityDomain.
     */
    Principal getCallerPrincipalSecurityDomain();

    /**
     * @return the caller principal obtained from the SecurityContext.
     */
    Principal getCallerPrincipalSecurityContext();

}
