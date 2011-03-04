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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.dmr.ModelNode;

/**
 * Model description for the domain root.
 *
 * @author Brian Stansberry
 */
public class DomainRootDescription {

    private static final String RESOURCE_NAME = DomainRootDescription.class.getPackage().getName() + ".LocalDescriptions";

    public static ModelNode getDescription(final Locale locale) {

        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("domain"));
        root.get(HEAD_COMMENT_ALLOWED).set(true);
        root.get(TAIL_COMMENT_ALLOWED).set(true);
        root.get(ATTRIBUTES, NAMESPACES).set(CommonDescriptions.getNamespacePrefixAttribute(locale));
        root.get(ATTRIBUTES, SCHEMA_LOCATIONS).set(CommonDescriptions.getSchemaLocationAttribute(locale));

        root.get(OPERATIONS).setEmptyObject();

        root.get(CHILDREN, EXTENSION, DESCRIPTION).set(bundle.getString("domain.extension"));
        root.get(CHILDREN, EXTENSION, MIN_OCCURS).set(0);
        root.get(CHILDREN, EXTENSION, MAX_OCCURS).set(Integer.MAX_VALUE);
        root.get(CHILDREN, EXTENSION, MODEL_DESCRIPTION).setEmptyObject();

        root.get(CHILDREN, PATH, DESCRIPTION).set(bundle.getString("domain.path"));
        root.get(CHILDREN, PATH, MIN_OCCURS).set(0);
        root.get(CHILDREN, PATH, MAX_OCCURS).set(Integer.MAX_VALUE);
        root.get(CHILDREN, PATH, MODEL_DESCRIPTION).setEmptyObject();

        root.get(CHILDREN, PROFILE, DESCRIPTION).set(bundle.getString("domain.profile"));
        root.get(CHILDREN, PROFILE, MIN_OCCURS).set(1);
        root.get(CHILDREN, PROFILE, MAX_OCCURS).set(Integer.MAX_VALUE);
        root.get(CHILDREN, PROFILE, MODEL_DESCRIPTION).setEmptyObject();

        root.get(CHILDREN, INTERFACE, DESCRIPTION).set(bundle.getString("domain.interface"));
        root.get(CHILDREN, INTERFACE, MIN_OCCURS).set(0);
        root.get(CHILDREN, INTERFACE, MAX_OCCURS).set(Integer.MAX_VALUE);
        root.get(CHILDREN, INTERFACE, MODEL_DESCRIPTION).setEmptyObject();

        root.get(CHILDREN, SOCKET_BINDING_GROUP, DESCRIPTION).set(bundle.getString("domain.socket-binding-group"));
        root.get(CHILDREN, SOCKET_BINDING_GROUP, MIN_OCCURS).set(0);
        root.get(CHILDREN, SOCKET_BINDING_GROUP, MAX_OCCURS).set(Integer.MAX_VALUE);
        root.get(CHILDREN, SOCKET_BINDING_GROUP, MODEL_DESCRIPTION).setEmptyObject();

        root.get(CHILDREN, SYSTEM_PROPERTY, DESCRIPTION).set(bundle.getString("domain.system-property"));
        root.get(CHILDREN, SYSTEM_PROPERTY, MIN_OCCURS).set(0);
        root.get(CHILDREN, SYSTEM_PROPERTY, MAX_OCCURS).set(Integer.MAX_VALUE);
        root.get(CHILDREN, SYSTEM_PROPERTY, MODEL_DESCRIPTION).setEmptyObject();

        root.get(CHILDREN, DEPLOYMENT, DESCRIPTION).set(bundle.getString("domain.deployment"));
        root.get(CHILDREN, DEPLOYMENT, MIN_OCCURS).set(0);
        root.get(CHILDREN, DEPLOYMENT, MAX_OCCURS).set(Integer.MAX_VALUE);
        root.get(CHILDREN, DEPLOYMENT, MODEL_DESCRIPTION).setEmptyObject();

        root.get(CHILDREN, SERVER_GROUP, DESCRIPTION).set(bundle.getString("domain.server-group"));
        root.get(CHILDREN, SERVER_GROUP, MIN_OCCURS).set(0);
        root.get(CHILDREN, SERVER_GROUP, MAX_OCCURS).set(Integer.MAX_VALUE);
        root.get(CHILDREN, SERVER_GROUP, MODEL_DESCRIPTION).setEmptyObject();

        root.get(CHILDREN, HOST, DESCRIPTION).set(bundle.getString("domain.host"));
        root.get(CHILDREN, HOST, MIN_OCCURS).set(0);
        root.get(CHILDREN, HOST, MAX_OCCURS).set(Integer.MAX_VALUE);
        root.get(CHILDREN, HOST, MODEL_DESCRIPTION).setEmptyObject();

        return root;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

}
