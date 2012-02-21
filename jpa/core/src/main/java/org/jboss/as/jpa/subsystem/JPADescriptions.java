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
 */
package org.jboss.as.jpa.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Model and operation descriptions for the JPA subsystem.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class JPADescriptions {

    static final String RESOURCE_NAME = JPADescriptions.class.getPackage().getName() + ".LocalDescriptions";

    private JPADescriptions() {
    }


    static ModelNode getSubsystemDescription(Locale locale) {

        ModelNode subsystem = new ModelNode();
        final ResourceBundle bundle = getResourceBundle(locale);
        subsystem.get(DESCRIPTION).set(bundle.getString("jpa"));
        subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
        subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
        subsystem.get(NAMESPACE).set(Namespace.JPA_1_0.getUriString());
        subsystem.get(ATTRIBUTES, CommonAttributes.DEFAULT_DATASOURCE, DESCRIPTION).set(bundle.getString("default.datasource"));
        subsystem.get(ATTRIBUTES, CommonAttributes.DEFAULT_DATASOURCE, TYPE).set(ModelType.STRING);
        subsystem.get(ATTRIBUTES, CommonAttributes.DEFAULT_DATASOURCE, REQUIRED).set(true);
        subsystem.get(ATTRIBUTES, CommonAttributes.DEFAULT_DATASOURCE, MIN_LENGTH).set(0);
        subsystem.get(ATTRIBUTES, CommonAttributes.DEFAULT_DATASOURCE, NILLABLE).set(true);
        subsystem.get(CHILDREN).setEmptyObject();
        return subsystem;
    }

    static ModelNode getSubsystemAdd(Locale locale) {

        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(JPASubSystemAdd.OPERATION_NAME);
        op.get(org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("jpa.add"));

        op.get(REQUEST_PROPERTIES, CommonAttributes.DEFAULT_DATASOURCE, DESCRIPTION).set(bundle.getString("default.datasource"));
        op.get(REQUEST_PROPERTIES, CommonAttributes.DEFAULT_DATASOURCE, TYPE).set(ModelType.STRING);
        op.get(REQUEST_PROPERTIES, CommonAttributes.DEFAULT_DATASOURCE, REQUIRED).set(true);
        op.get(REQUEST_PROPERTIES, CommonAttributes.DEFAULT_DATASOURCE, MIN_LENGTH).set(0);
        op.get(REQUEST_PROPERTIES, CommonAttributes.DEFAULT_DATASOURCE, NILLABLE).set(true);
        op.get(REPLY_PROPERTIES).setEmptyObject();

        return op;
    }

    static ModelNode getSubsystemRemove(Locale locale) {

        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(JPASubSystemRemove.OPERATION_NAME);
        op.get(DESCRIPTION).set(bundle.getString("jpa.remove"));

        op.get(REQUEST_PROPERTIES).setEmptyObject();

        op.get(REPLY_PROPERTIES).setEmptyObject();

        return op;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}
