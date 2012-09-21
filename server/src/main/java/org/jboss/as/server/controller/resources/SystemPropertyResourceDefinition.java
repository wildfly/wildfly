/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.server.controller.resources;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.ProcessEnvironmentSystemPropertyUpdater;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentSystemPropertyUpdater;
import org.jboss.as.server.controller.descriptions.ServerDescriptions;
import org.jboss.as.server.operations.SystemPropertyAddHandler;
import org.jboss.as.server.operations.SystemPropertyRemoveHandler;
import org.jboss.as.server.operations.SystemPropertyValueWriteAttributeHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SystemPropertyResourceDefinition extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition VALUE = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.VALUE, ModelType.STRING, true)
            .setAllowExpression(true)
            .setValidator(new StringLengthValidator(0, true, true))
            .build();

    public static final SimpleAttributeDefinition BOOT_TIME = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.BOOT_TIME, ModelType.BOOLEAN, true)
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, true))
            .setDefaultValue(new ModelNode(true))
            .build();

    static final AttributeDefinition[] ALL_ATTRIBUTES = new AttributeDefinition[] {VALUE, BOOT_TIME};
    static final AttributeDefinition[] SERVER_ATTRIBUTES = new AttributeDefinition[] {VALUE};

    final ProcessEnvironmentSystemPropertyUpdater systemPropertyUpdater;
    final boolean useBoottime;

    private SystemPropertyResourceDefinition(Location location, ProcessEnvironmentSystemPropertyUpdater systemPropertyUpdater, boolean useBoottime) {
        super(PathElement.pathElement(SYSTEM_PROPERTY),
                new ReplaceResourceNameResourceDescriptionResolver(location, SYSTEM_PROPERTY),
                new SystemPropertyAddHandler(systemPropertyUpdater, useBoottime, useBoottime ? ALL_ATTRIBUTES : SERVER_ATTRIBUTES),
                new SystemPropertyRemoveHandler(systemPropertyUpdater));
        this.systemPropertyUpdater = systemPropertyUpdater;
        this.useBoottime = useBoottime;
    }

    public static SystemPropertyResourceDefinition createForStandaloneServer(ServerEnvironment processEnvironment) {
        return new SystemPropertyResourceDefinition(Location.STANDALONE, new ServerEnvironmentSystemPropertyUpdater(processEnvironment), false);
    }

    public static SystemPropertyResourceDefinition createForDomainOrHost(Location location) {
        return new SystemPropertyResourceDefinition(location, null, true);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(VALUE, null, new SystemPropertyValueWriteAttributeHandler(systemPropertyUpdater, VALUE));
        if (useBoottime) {
            resourceRegistration.registerReadWriteAttribute(BOOT_TIME, null, new WriteAttributeHandlers.AttributeDefinitionValidatingHandler(BOOT_TIME));
        }
    }

    private static class ReplaceResourceNameResourceDescriptionResolver extends StandardResourceDescriptionResolver {
        Location location;
        public ReplaceResourceNameResourceDescriptionResolver(Location location, String keyPrefix) {
            super(keyPrefix, ServerDescriptions.RESOURCE_NAME, SecurityActions.getClassLoader(ServerDescriptions.class), true, false);
            this.location = location;
        }

        public String getResourceDescription(Locale locale, ResourceBundle bundle) {
            //TODO - there should be a better way
            return bundle.getString(SYSTEM_PROPERTY + "." + location.getSuffix());
        }
    }

    public enum Location {
        STANDALONE("server"),
        DOMAIN("domain"),
        HOST("host"),
        SERVER_CONFIG("server-config"),
        SERVER_GROUP("server-group");

        private String suffix;

        Location(String suffix){
            this.suffix = suffix;
        }

        String getSuffix() {
            return suffix;
        }
    }

}
