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

package org.jboss.as.domain.management.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Utility methods related to the management API for security realms.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ManagementUtil {

    static boolean isSecurityRealmReloadRequired(final OperationContext context, final ModelNode operation) {
        ServiceController<?> controller = getSecurityRealmService(context, operation, false);
        return isSecurityRealmReloadRequired(context, controller);
    }

    static boolean isSecurityRealmReloadRequired(final OperationContext context, final ServiceController<?> controller) {
        boolean reloadRequired = false;
        ServiceController.Substate substate = controller == null ? null : controller.getSubstate();
        if (substate != null && substate.isRestState() && substate.getState() == ServiceController.State.UP) {
            if (!context.isResourceServiceRestartAllowed()) {
                reloadRequired = true;
            }
        }

        return reloadRequired;
    }

    static ServiceController<?> getSecurityRealmService(final OperationContext context, final ModelNode operation, final boolean forUpdate) {
        final String realmName = getSecurityRealmName(operation);
        ServiceRegistry registry = context.getServiceRegistry(forUpdate);
        ServiceName svcName = SecurityRealmService.BASE_SERVICE_NAME.append(realmName);
        return registry.getService(svcName);
    }

    static String getSecurityRealmName(final ModelNode operation) {
        String realmName = null;
        PathAddress pa = PathAddress.pathAddress(operation.require(OP_ADDR));
        for (int i = pa.size() - 1; i > 0; i--) {
            PathElement pe = pa.getElement(i);
            if (SECURITY_REALM.equals(pe.getKey())) {
                realmName = pe.getValue();
                break;
            }
        }
        assert realmName != null : "operation did not have an address that included a " + SECURITY_REALM;
        return realmName;
    }

    static void updateUserDomainCallbackHandler(final OperationContext context, final ModelNode operation, final boolean forRollback) {
        UserDomainCallbackHandler cbh = getUserDomainCallbackHandler(context, operation);
        if (cbh != null) {
            PathAddress authAddress = getXmlAuthenticationAddress(operation);
            Resource root = forRollback ? context.getOriginalRootResource() : context.getRootResource();
            ModelNode userMap;
            try {
                Resource authResource = root.navigate(authAddress);
                userMap = context.resolveExpressions(Resource.Tools.readModel(authResource));
            } catch (Exception e) {
                userMap = new ModelNode().setEmptyObject();
            }
            cbh.setUserDomain(userMap);
        }
    }

    private static PathAddress getXmlAuthenticationAddress(ModelNode operation) {
        PathAddress base = PathAddress.pathAddress(operation.require(OP_ADDR));
        PathAddress result = null;
        for (int i = base.size() - 1; i >=0; i--) {
            PathElement pe = base.getElement(i);
            if (AUTHENTICATION.equals(pe.getKey())) {
                result = base.subAddress(0, i + 1);
                break;
            }
        }
        assert result != null : "operation did not point to a child of the xml authentication resource";
        return result;
    }

    private static UserDomainCallbackHandler getUserDomainCallbackHandler(final OperationContext context, final ModelNode operation) {
        final String realmName = getSecurityRealmName(operation);
        ServiceRegistry registry = context.getServiceRegistry(true);
        ServiceName svcName = SecurityRealmService.BASE_SERVICE_NAME.append(realmName).append(UserDomainCallbackHandler.SERVICE_SUFFIX);
        ServiceController<?> sc = registry.getService(svcName);
        return sc == null ? null : UserDomainCallbackHandler.class.cast(sc.getValue());
    }

    /** Prevent instantiation */
    private ManagementUtil() {
    }
}
