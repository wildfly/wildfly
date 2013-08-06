/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.audit;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.audit.ManagedAuditLogger;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.domain.management._private.DomainManagementResolver;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the management audit logging resource.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class AccessAuditResourceDefinition extends SimpleResourceDefinition {

    public static final PathElement PATH_ELEMENT = PathElement.pathElement(ModelDescriptionConstants.ACCESS, ModelDescriptionConstants.AUDIT);

    private final ManagedAuditLogger auditLogger;
    private final PathManagerService pathManager;
    private final EnvironmentNameReader environmentReader;

    public AccessAuditResourceDefinition(final ManagedAuditLogger auditLogger, final PathManagerService pathManager, final EnvironmentNameReader environmentReader) {
        super(
                PATH_ELEMENT,
                DomainManagementResolver.getResolver("core.management.audit-log"),
                new AbstractAddStepHandler() {
                    @Override
                    protected boolean requiresRuntime(OperationContext context) {
                        return false;
                    }},
                new AbstractRemoveStepHandler() {
                    @Override
                    protected boolean requiresRuntime(OperationContext context) {
                        return false;
                    }});
        this.auditLogger = auditLogger;
        this.pathManager = pathManager;
        this.environmentReader = environmentReader;
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new JsonAuditLogFormatterResourceDefinition(auditLogger));
        resourceRegistration.registerSubModel(new FileAuditLogHandlerResourceDefinition(auditLogger, pathManager));
        resourceRegistration.registerSubModel(new SyslogAuditLogHandlerResourceDefinition(auditLogger, pathManager, environmentReader));
        resourceRegistration.registerSubModel(AuditLogLoggerResourceDefinition.createDefinition(auditLogger));
        if (!environmentReader.isServer()){
            resourceRegistration.registerSubModel(AuditLogLoggerResourceDefinition.createHostServerDefinition(auditLogger));
        }
    }


}
