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
 * A {@link org.jboss.as.arquillian.api.ServerSetupTask} instance which creates security domains for this test case.
 *
 * @author Pedro Igor
 */
class JaspiSecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {

    static final String UNDERTOW_MODULE_NAME = "org.wildfly.extension.undertow";

    static final String SECURITY_DOMAIN_NAME = "jaspi-http-scheme-server-auth-module";

    @Override
    protected SecurityDomain[] getSecurityDomains() {
        String loginModuleStacksName = "lm-stack";

        return new SecurityDomain[] { new SecurityDomain.Builder().name(SECURITY_DOMAIN_NAME)
                .jaspiAuthn(new JaspiAuthn.Builder()
                        .loginModuleStacks(new LoginModuleStack.Builder()
                                .name(loginModuleStacksName)
                                .loginModules(new SecurityModule.Builder().name("UsersRoles").flag(Constants.REQUIRED).build())
                                .build())
                        .authnModules(new AuthnModule.Builder()
                                .name(HTTPSchemeServerAuthModule.class.getName())
                                .loginModuleStackRef(loginModuleStacksName)
                                .module(UNDERTOW_MODULE_NAME)
                                .build())
                        .build())
                .cacheType("default")
                .build() };
    }
}
