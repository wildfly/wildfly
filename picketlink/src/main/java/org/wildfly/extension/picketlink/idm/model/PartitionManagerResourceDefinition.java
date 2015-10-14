/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.picketlink.idm.model;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.idm.IDMExtension;

import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class PartitionManagerResourceDefinition extends AbstractIDMResourceDefinition {

    private static final List<AccessConstraintDefinition> CONSTRAINTS = new SensitiveTargetAccessConstraintDefinition(
        new SensitivityClassification(IDMExtension.SUBSYSTEM_NAME, "partition-manager", false, true, true)
    ).wrapAsList();

    public static final SimpleAttributeDefinition IDENTITY_MANAGEMENT_JNDI_URL = new SimpleAttributeDefinitionBuilder(ModelElement.IDENTITY_MANAGEMENT_JNDI_NAME.getName(), ModelType.STRING, false).setAllowExpression(true).build();
    public static final PartitionManagerResourceDefinition INSTANCE = new PartitionManagerResourceDefinition();

    private PartitionManagerResourceDefinition() {
        super(ModelElement.PARTITION_MANAGER, PartitionManagerAddHandler.INSTANCE,
                 PartitionManagerRemoveHandler.INSTANCE, IDENTITY_MANAGEMENT_JNDI_URL);
        setDeprecated(IDMExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        addChildResourceDefinition(IdentityConfigurationResourceDefinition.INSTANCE, resourceRegistration);
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return CONSTRAINTS;
    }
}
