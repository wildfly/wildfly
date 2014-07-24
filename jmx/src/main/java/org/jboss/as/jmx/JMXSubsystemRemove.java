/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jmx;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.access.management.JmxAuthorizer;
import org.jboss.as.controller.audit.ManagedAuditLogger;
import org.jboss.dmr.ModelNode;

/**
 * Removes the remoting subsystem
 *
 * @author Kabir Khan
 */
public class JMXSubsystemRemove extends AbstractRemoveStepHandler {

    private final ManagedAuditLogger auditLoggerInfo;
    private final JmxAuthorizer authorizer;

    JMXSubsystemRemove(ManagedAuditLogger auditLoggerInfo, JmxAuthorizer authorizer) {
        this.auditLoggerInfo = auditLoggerInfo;
        this.authorizer = authorizer;
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        if (isRemoveService(context, operation)) {
            //Since so many things can depend on this, only remove if the user set the ALLOW_RESOURCE_SERVICE_RESTART operation header
            context.removeService(MBeanServerService.SERVICE_NAME);
        } else {
            context.reloadRequired();
        }
    }

    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        if (isRemoveService(context, operation)) {
            JMXSubsystemAdd.launchServices(context, model, auditLoggerInfo, authorizer, null, null);
        } else {
            context.revertReloadRequired();
        }
    }

    private boolean isRemoveService(OperationContext context, ModelNode operation) {
        if (context.isNormalServer()) {
            if (context.isResourceServiceRestartAllowed()) {
                context.removeService(MBeanServerService.SERVICE_NAME);
                return true;
            }
        }
        return false;
    }
}
