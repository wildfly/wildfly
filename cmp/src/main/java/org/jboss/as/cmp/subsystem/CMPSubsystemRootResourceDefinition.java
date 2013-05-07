/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.as.cmp.subsystem;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * @author Stuart Douglas
 */
class CMPSubsystemRootResourceDefinition extends ModelOnlyResourceDefinition {

    static final CMPSubsystemRootResourceDefinition INSTANCE = new CMPSubsystemRootResourceDefinition();

    /** AttributeDefinition common to multiple child resources */
    static final SimpleAttributeDefinition JNDI_NAME =
            new SimpleAttributeDefinitionBuilder(CmpSubsystemModel.JNDI_NAME, ModelType.STRING, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    private CMPSubsystemRootResourceDefinition() {
        super(CmpSubsystemModel.SUBSYSTEM_PATH,
                CmpExtension.getResolver(CmpExtension.SUBSYSTEM_NAME));
    }

    @Override
    public void registerChildren(ManagementResourceRegistration subsystem) {

        subsystem.registerSubModel(new ModelOnlyResourceDefinition(CmpSubsystemModel.UUID_KEY_GENERATOR_PATH,
                CmpExtension.getResolver(CmpSubsystemModel.UUID_KEY_GENERATOR),
                JNDI_NAME));

        subsystem.registerSubModel(HiLoKeyGeneratorResourceDefinition.INSTANCE);
    }
}
