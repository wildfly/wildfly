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
package org.jboss.as.controller.descriptions.common;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.parsing.Attribute;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author anil saldhana
 */
public class VaultDescriptions {

    private static final String RESOURCE_NAME = InterfaceDescription.class.getPackage().getName() + ".LocalDescriptions";

    public static ModelNode getVaultDescriptions(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(TYPE).set(ModelType.LIST);
        node.get(DESCRIPTION).set(bundle.getString("vault"));
        node.get(REQUIRED).set(false);
        node.get(ATTRIBUTES, Attribute.CODE.getLocalName(), DESCRIPTION).set(bundle.getString("vault.code"));
        node.get(ATTRIBUTES, Attribute.CODE.getLocalName(), TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, NAME, REQUIRED).set(false);
        return node;
    }

    public static ModelNode getVaultAddDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("jvm.add"));
        node.get(REQUEST_PROPERTIES, TYPE, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, TYPE, DESCRIPTION).set(bundle.getString("jvm.type"));
        node.get(REQUEST_PROPERTIES, TYPE, REQUIRED).set(false);

        node.get(REQUEST_PROPERTIES, VAULT, DESCRIPTION).set(bundle.getString("vault"));
        node.get(REQUEST_PROPERTIES, VAULT, TYPE).set(ModelType.OBJECT);
        node.get(REQUEST_PROPERTIES, VAULT, REQUIRED).set(false);

        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    public static ModelNode getVaultRemoveDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(REMOVE);
        node.get(DESCRIPTION).set(bundle.getString("vault.remove"));
        node.get(REQUEST_PROPERTIES).setEmptyObject();
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}