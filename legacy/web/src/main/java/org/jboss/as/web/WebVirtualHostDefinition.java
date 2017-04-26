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

package org.jboss.as.web;

import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar
 * @created 23.2.12 16:17
 */
public class WebVirtualHostDefinition extends ModelOnlyResourceDefinition {

    protected static final SimpleAttributeDefinition NAME =
            new SimpleAttributeDefinitionBuilder(Constants.NAME, ModelType.STRING, false)
                    .setXmlName(Constants.NAME)
                    .setRequired(false)      // todo should be false, but 'add' won't validate then
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
    protected static final ListAttributeDefinition ALIAS =
            new StringListAttributeDefinition.Builder(Constants.ALIAS)
                    .setXmlName(Constants.ALIAS)
                    .setRequired(false)
                    .setElementValidator(new StringLengthValidator(1, false))
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
    protected static final SimpleAttributeDefinition DEFAULT_WEB_MODULE =
            new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_WEB_MODULE, ModelType.STRING, true)
                    .setXmlName(Constants.DEFAULT_WEB_MODULE)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setValidator(new StringLengthValidator(1, true, false))
                    .setDefaultValue(new ModelNode("ROOT.war"))
                    .build();
    protected static final SimpleAttributeDefinition ENABLE_WELCOME_ROOT =
            new SimpleAttributeDefinitionBuilder(Constants.ENABLE_WELCOME_ROOT, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.ENABLE_WELCOME_ROOT)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .build();
    static final WebVirtualHostDefinition INSTANCE = new WebVirtualHostDefinition();

    private WebVirtualHostDefinition() {
        super(WebExtension.HOST_PATH,
                WebExtension.getResourceDescriptionResolver(Constants.VIRTUAL_SERVER),
                new AddressToNameAddAdaptor(ALIAS, ENABLE_WELCOME_ROOT, DEFAULT_WEB_MODULE),
                ALIAS);
                setDeprecated(WebExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration hosts) {
        super.registerAttributes(hosts);
        hosts.registerReadOnlyAttribute(NAME, null);
        // They excluded each other...
        hosts.registerReadWriteAttribute(ENABLE_WELCOME_ROOT, null, WriteEnableWelcomeRoot.INSTANCE);
        hosts.registerReadWriteAttribute(DEFAULT_WEB_MODULE, null, WriteDefaultWebModule.INSTANCE);
    }
}
