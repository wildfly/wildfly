/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.integration.security.jaspi;

import org.jboss.as.security.Constants;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.config.AuthnModule;
import org.jboss.as.test.integration.security.common.config.JaspiAuthn;
import org.jboss.as.test.integration.security.common.config.LoginModuleStack;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.wildfly.extension.undertow.security.jaspi.modules.HTTPSchemeServerAuthModule;

/**
 * A {@link org.jboss.as.arquillian.api.ServerSetupTask} instance which creates security domains for test class JaspiFormAuthTestCase.
 *
 * @author Daniel Cihak
 */
public class JaspiSimpleServerLoginDomainSetup extends AbstractSecurityDomainsServerSetupTask {

    static final String UNDERTOW_MODULE_NAME = "org.wildfly.extension.undertow";

    static final String SECURITY_DOMAIN_NAME = "jaspi";

    @Override
    protected SecurityDomain[] getSecurityDomains() {
        String loginModuleStackName = "jaspi-stack";

        return new SecurityDomain[] { new SecurityDomain.Builder().name(SECURITY_DOMAIN_NAME)
                .jaspiAuthn(new JaspiAuthn.Builder()
                        .loginModuleStacks(new LoginModuleStack.Builder()
                                .name(loginModuleStackName)
                                .loginModules(new SecurityModule.Builder().name("org.jboss.security.auth.spi.SimpleServerLoginModule").flag(Constants.OPTIONAL).build())
                                .build())
                        .authnModules(new AuthnModule.Builder()
                                .name(HTTPSchemeServerAuthModule.class.getName())
                                .loginModuleStackRef(loginModuleStackName)
                                .module(UNDERTOW_MODULE_NAME)
                                .build())
                        .build())
                .cacheType("default")
                .build() };
    }
}
