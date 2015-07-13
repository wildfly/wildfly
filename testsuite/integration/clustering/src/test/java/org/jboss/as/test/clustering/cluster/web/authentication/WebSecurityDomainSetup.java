/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering.cluster.web.authentication;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.LOGIN_MODULE;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;

import java.util.Arrays;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.security.Constants;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

public class WebSecurityDomainSetup extends AbstractSecurityDomainSetup {

    private static final Logger log = Logger.getLogger(WebSecurityDomainSetup.class);

    private static final String WEB_SECURITY_DOMAIN = "authentication";

    @Override
    public void setup(final ManagementClient managementClient, final String containerId) {
        log.debug("start of the domain creation");

        PathAddress securityDomainAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, "security"), PathElement.pathElement(SECURITY_DOMAIN, getSecurityDomainName()));

        ModelNode addSecurityDomainOperation = Util.createAddOperation(securityDomainAddress);

        PathAddress authenticationAddress = securityDomainAddress.append(Constants.AUTHENTICATION, Constants.CLASSIC);
        ModelNode addAuthenticationOperation = Util.createAddOperation(authenticationAddress);

        ModelNode addLoginModuleOperation = Util.createAddOperation(authenticationAddress.append(LOGIN_MODULE, "UsersRoles"));
        addLoginModuleOperation.get(CODE).set("UsersRoles");
        addLoginModuleOperation.get(FLAG).set("required");
        addLoginModuleOperation.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);

        applyUpdates(managementClient.getControllerClient(), Arrays.asList(Operations.createCompositeOperation(addSecurityDomainOperation, addAuthenticationOperation, addLoginModuleOperation)));

        log.debug("end of the domain creation");
    }

    @Override
    protected String getSecurityDomainName() {
        return WEB_SECURITY_DOMAIN;
    }
}