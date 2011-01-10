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
 */package org.jboss.as.controller.descriptions.host;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.CommonAttributes;
import org.jboss.as.controller.descriptions.common.PathDescription;
import org.jboss.dmr.ModelNode;

/**
 * Model description for the host model root.
 *
 * @author Brian Stansberry
 */
public class HostRootDescription {

    private static final ModelNode ROOT = new ModelNode();
    static {
        // FIXME load descriptions from an external resource
        ROOT.get(DESCRIPTION).set("The root node of the host-level management model.");
        ROOT.get(HEAD_COMMENT_ALLOWED).set(true);
        ROOT.get(TAIL_COMMENT_ALLOWED).set(true);
        ROOT.get(ATTRIBUTES, NAMESPACES).set(CommonAttributes.getNamespacePrefixAttribute());
        ROOT.get(ATTRIBUTES, SCHEMA_LOCATIONS).set(CommonAttributes.getSchemaLocationAttribute());

        ROOT.get(ATTRIBUTES, "management").set("TODO");  // FIXME Management
        ROOT.get(ATTRIBUTES, "domain-controller").set("TODO");// FIXME DomainController

        ROOT.get(CHILDREN, EXTENSION, DESCRIPTION).set("A list of extension modules.");
        ROOT.get(CHILDREN, EXTENSION, MIN_OCCURS).set(0);
        ROOT.get(CHILDREN, EXTENSION, MAX_OCCURS).set(Integer.MAX_VALUE);
        ROOT.get(CHILDREN, EXTENSION, MODEL_DESCRIPTION).setEmptyObject();

        ROOT.get(CHILDREN, PATH, DESCRIPTION).set("A list of named filesystem paths.");
        ROOT.get(CHILDREN, PATH, MIN_OCCURS).set(0);
        ROOT.get(CHILDREN, PATH, MAX_OCCURS).set(Integer.MAX_VALUE);
        ROOT.get(CHILDREN, PATH, MODEL_DESCRIPTION).setEmptyObject();

        ROOT.get(CHILDREN, SYSTEM_PROPERTY, DESCRIPTION).set("A list of system properties to set on all servers on the host.");
        ROOT.get(CHILDREN, SYSTEM_PROPERTY, MIN_OCCURS).set(0);
        ROOT.get(CHILDREN, SYSTEM_PROPERTY, MAX_OCCURS).set(Integer.MAX_VALUE);
        ROOT.get(CHILDREN, SYSTEM_PROPERTY, MODEL_DESCRIPTION).setEmptyObject();

        ROOT.get(CHILDREN, INTERFACE, DESCRIPTION).set("A list of fully specified named network interfaces available for use on the host.");
        ROOT.get(CHILDREN, INTERFACE, MIN_OCCURS).set(0);
        ROOT.get(CHILDREN, INTERFACE, MAX_OCCURS).set(Integer.MAX_VALUE);
        ROOT.get(CHILDREN, INTERFACE, MODEL_DESCRIPTION).setEmptyObject();

        ROOT.get(CHILDREN, JVM, DESCRIPTION).set("A list of Java Virtual Machine configurations that can be applied ot servers on the host.");
        ROOT.get(CHILDREN, JVM, MIN_OCCURS).set(0);
        ROOT.get(CHILDREN, JVM, MAX_OCCURS).set(Integer.MAX_VALUE);
        ROOT.get(CHILDREN, JVM, MODEL_DESCRIPTION).setEmptyObject();

        ROOT.get(CHILDREN, SERVER, DESCRIPTION).set("A list of server configurations.");
        ROOT.get(CHILDREN, SERVER, MIN_OCCURS).set(0);
        ROOT.get(CHILDREN, SERVER, MAX_OCCURS).set(Integer.MAX_VALUE);
        ROOT.get(CHILDREN, SERVER, MODEL_DESCRIPTION).setEmptyObject();
    }

    public static ModelNode getDescription(final boolean recursive) {
        final ModelNode root = ROOT.clone();
        if (recursive) {
            root.get(CHILDREN, EXTENSION, MODEL_DESCRIPTION).set("TODO");  // TODO fill out EXTENSION
            root.get(CHILDREN, PATH, MODEL_DESCRIPTION).set(PathDescription.getSpecifiedPathDescription());
            root.get(CHILDREN, SYSTEM_PROPERTY, MODEL_DESCRIPTION).set("TODO");  // TODO fill out SYSTEM_PROPERTY
            root.get(CHILDREN, INTERFACE, MODEL_DESCRIPTION).set("TODO");  // TODO fill out INTERFACE
            root.get(CHILDREN, JVM, MODEL_DESCRIPTION).set("TODO");  // TODO fill out JVM
            root.get(CHILDREN, SERVER, MODEL_DESCRIPTION).set("TODO");  // TODO fill out SERVER
        }
        return root;
    }

    public static void main(final String[] args) {
        final ModelNode root = getDescription(true);
        root.get(ModelDescriptionConstants.OPERATIONS).add(PathDescription.getSpecifiedPathAddOperation());
        root.get(ModelDescriptionConstants.OPERATIONS).add(PathDescription.getPathRemoveOperation());
        root.get(CHILDREN, PATH, ModelDescriptionConstants.OPERATIONS).add(PathDescription.getSetSpecifiedPathOperation());
        root.get(CHILDREN, PATH, ModelDescriptionConstants.OPERATIONS).add(PathDescription.getSetRelativeToOperation());
        System.out.println(root);
    }

}
