/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.AUTHENTICATION;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.security.Constants;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.dmr.ModelNode;

/**
 * Utility methods to create/remove simple security domains
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class EjbSecurityDomainSetup extends AbstractSecurityDomainSetup {

    protected static final String DEFAULT_SECURITY_DOMAIN_NAME = "ejb3-tests";

    @Override
    protected String getSecurityDomainName() {
        return DEFAULT_SECURITY_DOMAIN_NAME;
    }

    public boolean isUsersRolesRequired() {
        return true;
    }

    @Override
    public void setup(final ManagementClient managementClient, final String containerId) throws Exception {

        final ModelNode compositeOp = new ModelNode();
        compositeOp.get(OP).set(COMPOSITE);
        compositeOp.get(OP_ADDR).setEmptyList();

        ModelNode steps = compositeOp.get(STEPS);
        PathAddress securityDomainAddress = PathAddress.pathAddress()
                .append(SUBSYSTEM, "security")
                .append(SECURITY_DOMAIN, getSecurityDomainName());
        steps.add(Util.createAddOperation(securityDomainAddress));
        PathAddress authAddress = securityDomainAddress.append(AUTHENTICATION, Constants.CLASSIC);
        steps.add(Util.createAddOperation(authAddress));
        ModelNode op = Util.createAddOperation(authAddress.append(Constants.LOGIN_MODULE, "Remoting"));
        op.get(CODE).set("Remoting");
        if (isUsersRolesRequired()) {
            op.get(FLAG).set("optional");
        } else {
            op.get(FLAG).set("required");
        }
        op.get(Constants.MODULE_OPTIONS).add("password-stacking", "useFirstPass");
        steps.add(op);
        if (isUsersRolesRequired()) {

            ModelNode loginModule = Util.createAddOperation(authAddress.append(Constants.LOGIN_MODULE, "UsersRoles"));
            loginModule.get(CODE).set("UsersRoles");
            loginModule.get(FLAG).set("required");
            loginModule.get(Constants.MODULE_OPTIONS).add("password-stacking", "useFirstPass");
            loginModule.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            steps.add(loginModule);
        }

        System.out.println(compositeOp);
        applyUpdates(managementClient.getControllerClient(), Arrays.asList(compositeOp));
    }
}
