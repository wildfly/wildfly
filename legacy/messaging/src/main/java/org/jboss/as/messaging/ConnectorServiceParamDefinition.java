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

package org.jboss.as.messaging;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.dmr.ModelType;

/**
 * Connector service resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class ConnectorServiceParamDefinition extends ModelOnlyResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(CommonAttributes.PARAM);

    public static final SimpleAttributeDefinition VALUE = create("value", ModelType.STRING)
            .setRequired(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = { VALUE };

    static final ConnectorServiceParamDefinition INSTANCE = new ConnectorServiceParamDefinition();

    private ConnectorServiceParamDefinition() {
        super(PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.CONNECTOR_SERVICE, CommonAttributes.PARAM),
                ATTRIBUTES);
        setDeprecated(MessagingExtension.DEPRECATED_SINCE);
    }
}