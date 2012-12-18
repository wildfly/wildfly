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

package org.jboss.as.cmp.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Stuart Douglas
 */
public class UUIDKeyGeneratorResourceDescription extends SimpleResourceDefinition {

    public static final UUIDKeyGeneratorResourceDescription INSTANCE = new UUIDKeyGeneratorResourceDescription();

    protected static final SimpleAttributeDefinition JNDI_NAME = new SimpleAttributeDefinitionBuilder(CmpSubsystemModel.JNDI_NAME, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    public static final Map<String, SimpleAttributeDefinition> ATTRIBUTE_MAP;
    public static final SimpleAttributeDefinition[] ATTRIBUTES = {JNDI_NAME};

    static {
        Map<String, SimpleAttributeDefinition> map = new LinkedHashMap<String, SimpleAttributeDefinition>();
        map.put(JNDI_NAME.getName(), JNDI_NAME);
        ATTRIBUTE_MAP = Collections.unmodifiableMap(map);
    }

    private UUIDKeyGeneratorResourceDescription() {
        super(CmpSubsystemModel.UUID_KEY_GENERATOR_PATH,
                CmpExtension.getResourceDescriptionResolver(CmpSubsystemModel.UUID_KEY_GENERATOR),
                UUIDKeyGeneratorAdd.INSTANCE, UUIDKeyGeneratorRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTE_MAP.values()) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }
    }
}
