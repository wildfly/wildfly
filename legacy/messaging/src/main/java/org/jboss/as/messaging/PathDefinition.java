/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
import static org.jboss.as.messaging.CommonAttributes.BINDINGS_DIRECTORY;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_DIRECTORY;
import static org.jboss.as.messaging.CommonAttributes.LARGE_MESSAGES_DIRECTORY;
import static org.jboss.as.messaging.CommonAttributes.PAGING_DIRECTORY;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class PathDefinition extends ModelOnlyResourceDefinition {

    static final String DEFAULT_RELATIVE_TO = ServerEnvironment.SERVER_DATA_DIR;

    // base attribute for the 4 messaging path subresources.
    // each one define a different default values. Their respective attributes are accessed through the PATHS map.
    private static final SimpleAttributeDefinition PATH_BASE = create(PathResourceDefinition.PATH)
            .setAllowExpression(true)
            .setRequired(false)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition RELATIVE_TO = create(PathResourceDefinition.RELATIVE_TO)
            .setDefaultValue(new ModelNode(DEFAULT_RELATIVE_TO))
            .setRequired(false)
            .setRestartAllServices()
            .build();

    public static final Map<String, SimpleAttributeDefinition> PATHS = new HashMap<String, SimpleAttributeDefinition>();

    private static final String DEFAULT_PATH = "messaging";
    // all default paths dir are prepended with messaging
    // I am not sure this was not a typo and that they should have been put inside a messaging/ dir instead (as it
    // was stated in LocalDescriptions.properties)
    // For compatibility sake, we keep the messaging prefix.
    static final String DEFAULT_BINDINGS_DIR = DEFAULT_PATH + "bindings";
    static final String DEFAULT_JOURNAL_DIR = DEFAULT_PATH + "journal";
    static final String DEFAULT_LARGE_MESSAGE_DIR = DEFAULT_PATH + "largemessages";
    static final String DEFAULT_PAGING_DIR = DEFAULT_PATH + "paging";

    static {
        PATHS.put(BINDINGS_DIRECTORY, create(PATH_BASE).setDefaultValue(new ModelNode(DEFAULT_BINDINGS_DIR)).build());
        PATHS.put(JOURNAL_DIRECTORY, create(PATH_BASE).setDefaultValue(new ModelNode(DEFAULT_JOURNAL_DIR)).build());
        PATHS.put(LARGE_MESSAGES_DIRECTORY, create(PATH_BASE).setDefaultValue(new ModelNode(DEFAULT_LARGE_MESSAGE_DIR)).build());
        PATHS.put(PAGING_DIRECTORY, create(PATH_BASE).setDefaultValue(new ModelNode(DEFAULT_PAGING_DIR)).build());
    }

    static final AttributeDefinition[] getAttributes(final String path) {
        return new AttributeDefinition[] { PATHS.get(path), RELATIVE_TO };
    }

    public PathDefinition(PathElement path) {
        super(path,
                MessagingExtension.getResourceDescriptionResolver(ModelDescriptionConstants.PATH),
                getAttributes(path.getValue()));
    }

    // TODO add @Override once the WFCORE version with this method is integrated
    public int getMinOccurs() {
        return 1;
    }
}
