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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Stuart Douglas
 */
class UUIDKeyGeneratorResourceDefinition extends AbstractKeyGeneratorResourceDefinition {

    public static final UUIDKeyGeneratorResourceDefinition INSTANCE = new UUIDKeyGeneratorResourceDefinition();

    public static final Map<String, SimpleAttributeDefinition> ATTRIBUTE_MAP;
    public static final SimpleAttributeDefinition[] ATTRIBUTES;

    static {
        List<SimpleAttributeDefinition> list = new ArrayList<SimpleAttributeDefinition>(COMMON_ATTRIBUTES.length + 10);
        Collections.addAll(list, COMMON_ATTRIBUTES);
        ATTRIBUTES = list.toArray(new SimpleAttributeDefinition[list.size()]);

        Map<String, SimpleAttributeDefinition> map = new LinkedHashMap<String, SimpleAttributeDefinition>();
        for(SimpleAttributeDefinition ad : ATTRIBUTES) {
            map.put(ad.getName(), ad);
        }
        ATTRIBUTE_MAP = Collections.unmodifiableMap(map);
    }

    private UUIDKeyGeneratorResourceDefinition() {
        super(CmpSubsystemModel.UUID_KEY_GENERATOR_PATH,
                CmpExtension.getResolver(CmpSubsystemModel.UUID_KEY_GENERATOR),
                UUIDKeyGeneratorAdd.INSTANCE, UUIDKeyGeneratorRemove.INSTANCE);
        setDeprecated(CmpExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTE_MAP.values()) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }
    }

    @Override
    public Map<String, SimpleAttributeDefinition> getAttributeMap() {
        return ATTRIBUTE_MAP;
    }
}
