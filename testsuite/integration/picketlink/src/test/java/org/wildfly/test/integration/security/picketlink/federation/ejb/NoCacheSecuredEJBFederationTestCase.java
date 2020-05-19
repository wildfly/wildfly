/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.wildfly.test.integration.security.picketlink.federation.ejb;

import java.util.HashMap;
import java.util.Map;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.junit.runner.RunWith;

/**
 *
 * Test for issue: https://issues.jboss.org/browse/PLINK-793 Exact steps are described in issue.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ AbstractSecuredEJBFederationTestCase.PropertyFilesSetup.class, NoCacheSecuredEJBFederationTestCase.SecurityDomainsSetup.class })
public class NoCacheSecuredEJBFederationTestCase extends AbstractSecuredEJBFederationTestCase {

    static class SecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {

        /**
         * Returns SecurityDomains configuration for this testcase.
         *
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask#getSecurityDomains()
         */
        @Override
        protected SecurityDomain[] getSecurityDomains() {

            final Map<String, String> lmOptions = new HashMap<String, String>();
            final SecurityModule.Builder loginModuleBuilder = new SecurityModule.Builder().name("UsersRoles")
                    .options(lmOptions);

            lmOptions.put("usersProperties", PropertyFilesSetup.FILE_USERS.getAbsolutePath());
            lmOptions.put("rolesProperties", PropertyFilesSetup.FILE_ROLES.getAbsolutePath());
            final SecurityDomain idp = new SecurityDomain.Builder().name("ejb-idp").loginModules(loginModuleBuilder.build())
                    .build();

            lmOptions.remove("usersProperties");
            lmOptions.remove("rolesProperties");

            final SecurityModule.Builder samlLoginModuleBuilder = new SecurityModule.Builder()
                    .name("org.picketlink.identity.federation.bindings.jboss.auth.SAML2LoginModule");
            final SecurityDomain sp = new SecurityDomain.Builder().name("ejb-sp").loginModules(samlLoginModuleBuilder.build())
                    .build();
            final SecurityModule.Builder ejbModuleBuilder = new SecurityModule.Builder().name("Delegating").flag("required");
            final SecurityDomain ejbsp = new SecurityDomain.Builder().name("jboss-ejb-policy").cacheType("default")
                    .authorizationModules(ejbModuleBuilder.build()).build();

            return new SecurityDomain[] { idp, sp, ejbsp };
        }
    }

}
