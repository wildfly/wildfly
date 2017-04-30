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

import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar
 * @created 23.2.12 16:34
 */
public class WebAccessLogDirectoryDefinition extends ModelOnlyResourceDefinition {
    protected static final SimpleAttributeDefinition RELATIVE_TO =
            new SimpleAttributeDefinitionBuilder(Constants.RELATIVE_TO, ModelType.STRING)
                    .setXmlName(Constants.RELATIVE_TO)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .setDefaultValue(new ModelNode("jboss.server.log.dir"))
                    .build();
    protected static final SimpleAttributeDefinition PATH =
            new SimpleAttributeDefinitionBuilder(Constants.PATH, ModelType.STRING)
                    .setXmlName(Constants.PATH)
                    .setRequired(false)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true, true))
                    .build();
    static final WebAccessLogDirectoryDefinition INSTANCE = new WebAccessLogDirectoryDefinition();

    private WebAccessLogDirectoryDefinition() {
        super(WebExtension.DIRECTORY_PATH,
                WebExtension.getResourceDescriptionResolver("virtual-server.access-log.directory"),
                RELATIVE_TO, PATH);
                setDeprecated(WebExtension.DEPRECATED_SINCE);
    }

}
