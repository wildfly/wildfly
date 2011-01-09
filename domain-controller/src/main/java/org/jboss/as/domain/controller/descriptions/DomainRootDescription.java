/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
 */package org.jboss.as.domain.controller.descriptions;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;

import org.jboss.as.controller.descriptions.common.CommonAttributes;
import org.jboss.as.controller.descriptions.common.PathDescription;
import org.jboss.dmr.ModelNode;

/**
 * Model description for the domain root.
 *
 * @author Brian Stansberry
 */
public class DomainRootDescription {

    private static final ModelNode ROOT = new ModelNode();
    static {
        // FIXME load descriptions from an external resource
        ROOT.get(DESCRIPTION).set("The root node of the domain-level management model. This node should be addressed via the key/value pair \"base => domain\"");
        ROOT.get(ATTRIBUTES).get(NAMESPACES).set(CommonAttributes.getNamespacePrefixAttribute());
        ROOT.get(ATTRIBUTES).get(SCHEMA_LOCATIONS).set(CommonAttributes.getSchemaLocationAttribute());
        ROOT.get(CHILDREN, PATH, MIN_OCCURS).set(0);
        ROOT.get(CHILDREN, PATH, MAX_OCCURS).set(Integer.MAX_VALUE);
        ROOT.get(CHILDREN, PATH, MODEL_DESCRIPTION).setEmptyObject();
        ROOT.get(CHILDREN, PROFILE).setEmptyObject(); // TODO fill out PROFILE
        ROOT.get(CHILDREN, INTERFACE).setEmptyObject(); // TODO fill out INTERFACE
        ROOT.get(CHILDREN, SOCKET_BINDING_GROUP).setEmptyObject(); // TODO fill out SOCKET_BINDING_GROUP
        ROOT.get(CHILDREN, DEPLOYMENT).setEmptyObject(); // TODO fill out DEPLOYMENT
        ROOT.get(CHILDREN, SERVER_GROUP).setEmptyObject(); // TODO fill out SERVER_GROUP
        ROOT.get(CHILDREN, HOST).setEmptyObject(); // TODO fill out HOST
        ROOT.get(CHILDREN, SERVER).setEmptyObject(); // TODO fill out SERVER
    }

    public static ModelNode getDescription(final boolean recursive) {
        final ModelNode root = ROOT.clone();
        if (recursive) {
            root.get(CHILDREN, PATH, MODEL_DESCRIPTION).set(PathDescription.getNamedPathDescription());
        }
        return root;
    }

    public static void main(final String[] args) {
        System.out.println(getDescription(true));
    }

}
