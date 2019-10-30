/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.datasources.agroal;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.wildfly.extension.datasources.agroal.AgroalExtension.getResolver;

import java.util.Collection;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;

/**
 * Definition for the xa-datasource resource
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
class XADataSourceDefinition extends AbstractDataSourceDefinition {

    static final Collection<AttributeDefinition> ATTRIBUTES = unmodifiableList(asList(JNDI_NAME_ATTRIBUTE, STATISTICS_ENABLED_ATTRIBUTE, CONNECTION_FACTORY_ATTRIBUTE, CONNECTION_POOL_ATTRIBUTE));

    static final XADataSourceDefinition INSTANCE = new XADataSourceDefinition();

    // --- //

    private XADataSourceDefinition() {
        // TODO The cast to PersistentResourceDefinition.Parameters is a workaround to WFCORE-4040
        super((Parameters) new Parameters(pathElement("xa-datasource"), getResolver("xa-datasource"))
                .setAddHandler(XADataSourceOperations.ADD_OPERATION)
                .setRemoveHandler(XADataSourceOperations.REMOVE_OPERATION)
                .setAccessConstraints(new ApplicationTypeAccessConstraintDefinition(
                        new ApplicationTypeConfig(AgroalExtension.SUBSYSTEM_NAME, "xa-datasource"))));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }
}
