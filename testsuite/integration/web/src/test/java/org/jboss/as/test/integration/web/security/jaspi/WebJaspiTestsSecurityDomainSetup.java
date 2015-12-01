/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.web.security.jaspi;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.security.Constants;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.wildfly.extension.undertow.security.jaspi.modules.HTTPSchemeServerAuthModule;

import java.util.Arrays;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.security.Constants.*;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.MODULE;

/**
 * Creates SecurityDomain for JASPI auth tests. Concrete classes use different AuthModules.
 *
 * @author <a href="mailto:bspyrkos@redhat.com">Bartosz Spyrko-Smietanko</a>
 */
public abstract class WebJaspiTestsSecurityDomainSetup extends AbstractSecurityDomainSetup {

    private static final Logger log = Logger.getLogger(WebJaspiTestsSecurityDomainSetup.class);

    protected static final String WEB_SECURITY_DOMAIN = "web-tests";
    protected final String authModuleClassName;

    public WebJaspiTestsSecurityDomainSetup(String authModuleClassName) {
        this.authModuleClassName = authModuleClassName;
    }

    @Override
    public void setup(final ManagementClient managementClient, final String containerId) {
        log.debug("start of the domain creation");

        final ModelNode compositeOp = new ModelNode();
        compositeOp.get(OP).set(COMPOSITE);
        compositeOp.get(OP_ADDR).setEmptyList();

        ModelNode steps = compositeOp.get(STEPS);
        PathAddress address = PathAddress.pathAddress()
                .append(SUBSYSTEM, "security")
                .append(SECURITY_DOMAIN, getSecurityDomainName());

        steps.add(Util.createAddOperation(address));
        address = address.append(Constants.AUTHENTICATION, "jaspi");
        steps.add(Util.createAddOperation(address));
        ModelNode loginModuleStack = Util.createAddOperation(address.append(LOGIN_MODULE_STACK, "lm-stack"));
        loginModuleStack.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        steps.add(loginModuleStack);

        ModelNode loginModule = Util.createAddOperation(address.append(LOGIN_MODULE_STACK, "lm-stack").append(LOGIN_MODULE, "UsersRoles"));
        loginModule.get(CODE).set("UsersRoles");
        loginModule.get(FLAG).set("required");
        loginModule.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        steps.add(loginModule);

        final ModelNode authModule = Util.createAddOperation(address.append(AUTH_MODULE, authModuleClassName));
        authModule.get(CODE).set(authModuleClassName);
        authModule.get(MODULE).set("org.wildfly.extension.undertow");
        authModule.get(LOGIN_MODULE_STACK_REF).set("lm-stack");
        authModule.get(FLAG).set("required");
        authModule.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        steps.add(authModule);

        applyUpdates(managementClient.getControllerClient(), Arrays.asList(compositeOp));
        log.debug("end of the domain creation");
    }

    @Override
    protected String getSecurityDomainName() {
        return WEB_SECURITY_DOMAIN;
    }


    /**
     * Creates SecurityDomain for JASPI auth using a default AuthModule.
     *
     * @author <a href="mailto:bspyrkos@redhat.com">Bartosz Spyrko-Smietanko</a>
     */
    public static class WithDefaultAuthModule extends WebJaspiTestsSecurityDomainSetup {
        public WithDefaultAuthModule() {
            super(HTTPSchemeServerAuthModule.class.getName());
        }
    }

    /**
     * Creates SecurityDomain for JASPI auth using an always-failing AuthModule
     *
     * @author <a href="mailto:bspyrkos@redhat.com">Bartosz Spyrko-Smietanko</a>
     */
    public static class WithFailingAuthModule extends WebJaspiTestsSecurityDomainSetup {
        public WithFailingAuthModule() {
            super(FailingAuthModule.class.getName());
        }
    }
}
