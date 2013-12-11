/**
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

package org.wildfly.extension.mod_cluster.undertow;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * @author Radoslav Husar
 * @version Dec 2013
 * @since 8.0
 */
public class ModClusterUndertowResourceDefinition extends PersistentResourceDefinition {

    static final ModClusterUndertowResourceDefinition INSTANCE = new ModClusterUndertowResourceDefinition();

    private ModClusterUndertowResourceDefinition() {
        super(ModClusterUndertowExtension.SUBSYSTEM_PATH,
                ModClusterUndertowExtension.getResourceDescriptionResolver(),
                ModClusterUndertowSubsystemAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    protected static final SimpleAttributeDefinition LISTENER =
            new SimpleAttributeDefinitionBuilder(Element.LISTENER.getLocalName(), ModelType.STRING)
                    .setAllowExpression(true)
                    .setAllowNull(false)

                    .setRestartAllServices()
                    .setXmlName(Element.LISTENER.getLocalName())
                    .build();

    public static final AttributeDefinition[] ATTRIBUTES = {LISTENER};

    @Override
    public void registerAttributes(final ManagementResourceRegistration rootResourceRegistration) {
        ReloadRequiredWriteAttributeHandler handler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attribute : ATTRIBUTES) {
            rootResourceRegistration.registerReadWriteAttribute(attribute, null, handler);
        }
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

}
