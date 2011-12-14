/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jaxrs;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.dmr.ModelNode;


/**
 * @author Emanuel Muckenhuber
 */
class JaxrsSubsystemProviders {

    static final String RESOURCE_NAME = JaxrsSubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";

    static ModelNode getSubsystemDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode subsystem = new ModelNode();
        subsystem.get(DESCRIPTION).set(bundle.getString("jaxrs"));
        subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
        subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
        subsystem.get(NAMESPACE).set(JaxrsExtension.NAMESPACE);

        subsystem.get(ATTRIBUTES).setEmptyObject();
        subsystem.get(CHILDREN).setEmptyObject();
        subsystem.get(OPERATIONS);

        return subsystem;
    }

    static ModelNode getSubsystemAddDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(ADD);
        op.get(DESCRIPTION).set(bundle.getString("jaxrs.add"));
        op.get(REPLY_PROPERTIES).setEmptyObject();
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getSubsystemRemoveDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(REMOVE);
        op.get(DESCRIPTION).set(bundle.getString("jaxrs.remove"));
        op.get(REPLY_PROPERTIES).setEmptyObject();
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        return op;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}
