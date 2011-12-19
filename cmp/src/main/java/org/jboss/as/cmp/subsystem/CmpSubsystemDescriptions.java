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

package org.jboss.as.cmp.subsystem;

import java.util.Locale;
import java.util.ResourceBundle;
import static org.jboss.as.cmp.subsystem.CmpConstants.HILO_KEY_GENERATOR;
import static org.jboss.as.cmp.subsystem.CmpConstants.UUID_KEY_GENERATOR;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.threads.CommonAttributes.BLOCKING;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author John Bailey
 */
public class CmpSubsystemDescriptions {

    static final String RESOURCE_NAME = CmpSubsystemDescriptions.class.getPackage().getName() + ".LocalDescriptions";

    static final ModelNode getSubystemDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode subsystem = new ModelNode();
        subsystem.get(DESCRIPTION).set(bundle.getString("cmp"));
        subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
        subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
        subsystem.get(NAMESPACE).set(Namespace.CMP_1_0.getUriString());

        subsystem.get(CHILDREN, UUID_KEY_GENERATOR, DESCRIPTION).set(bundle.getString("uuid-key-generator"));
        subsystem.get(CHILDREN, UUID_KEY_GENERATOR, MIN_OCCURS).set(0);
        subsystem.get(CHILDREN, UUID_KEY_GENERATOR, MAX_OCCURS).set(Integer.MAX_VALUE);

        subsystem.get(CHILDREN, HILO_KEY_GENERATOR, DESCRIPTION).set(bundle.getString("hilo-key-generator"));
        subsystem.get(CHILDREN, HILO_KEY_GENERATOR, MIN_OCCURS).set(0);
        subsystem.get(CHILDREN, HILO_KEY_GENERATOR, MAX_OCCURS).set(Integer.MAX_VALUE);

        subsystem.get(OPERATIONS);

        return subsystem;
    }

    static ModelNode getSubsystemRemoveDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(REMOVE);
        op.get(DESCRIPTION).set(bundle.getString("cmp.remove"));
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

    public static ModelNode getSubystemAddDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(ADD);
        op.get(DESCRIPTION).set(bundle.getString("cmp.add"));

        op.get(REPLY_PROPERTIES).setEmptyObject();

        return op;
    }

    public static ModelNode getHiLoKeyGeneratorDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set(bundle.getString("hilo-key-generator"));
        node.get(HEAD_COMMENT_ALLOWED).set(true);
        node.get(TAIL_COMMENT_ALLOWED).set(true);

        for (AttributeDefinition attr : HiLoAttributeDefinitions.HILO_ATTRIBUTES) {
            attr.addResourceAttributeDescription(bundle, "hilo-key-generator", node);
        }

        return node;
    }

    public static ModelNode getUuidKeyGeneratorDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set(bundle.getString("uuid-key-generator"));
        node.get(HEAD_COMMENT_ALLOWED).set(true);
        node.get(TAIL_COMMENT_ALLOWED).set(true);
        return node;
    }

    public static ModelNode getHiLoAddDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode operation = new ModelNode();
        operation.get(OPERATION_NAME).set(ADD);
        operation.get(DESCRIPTION).set("hilo-key-generator.add");

        for (AttributeDefinition attr : HiLoAttributeDefinitions.HILO_ATTRIBUTES) {
            operation.get(REQUEST_PROPERTIES, attr.getName(), DESCRIPTION).set(bundle.getString("hilo-key-generator." + attr.getName()));
            operation.get(REQUEST_PROPERTIES, attr.getName(), TYPE).set(attr.getType());
            operation.get(REQUEST_PROPERTIES, attr.getName(), REQUIRED).set(false);
        }
        return operation;
    }

    public static ModelNode getHiLoRemoveDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode operation = new ModelNode();
        operation.get(OPERATION_NAME).set(REMOVE);
        operation.get(DESCRIPTION).set("hilo-key-generator.remove");
        return operation;
    }

    public static ModelNode getUuidAddDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode operation = new ModelNode();
        operation.get(OPERATION_NAME).set(ADD);
        operation.get(DESCRIPTION).set("uuid-key-generator.add");
        return operation;
    }

    public static ModelNode getUuidRemoveDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode operation = new ModelNode();
        operation.get(OPERATION_NAME).set(REMOVE);
        operation.get(DESCRIPTION).set("uuid-key-generator.remove");
        return operation;
    }

    private interface HiLoAttributeDefinitions {
        SimpleAttributeDefinition BLOCK_SIZE = new SimpleAttributeDefinition(CmpConstants.BLOCK_SIZE, ModelType.LONG, true);
        SimpleAttributeDefinition CREATE_TABLE = new SimpleAttributeDefinition(CmpConstants.CREATE_TABLE, ModelType.BOOLEAN, true);
        SimpleAttributeDefinition CREATE_TABLE_DDL = new SimpleAttributeDefinition(CmpConstants.CREATE_TABLE_DDL, ModelType.STRING, true);
        SimpleAttributeDefinition DATA_SOURCE = new SimpleAttributeDefinition(CmpConstants.DATA_SOURCE, ModelType.STRING, true);
        SimpleAttributeDefinition DROP_TABLE = new SimpleAttributeDefinition(CmpConstants.DROP_TABLE, ModelType.BOOLEAN, true);
        SimpleAttributeDefinition ID_COLUMN = new SimpleAttributeDefinition(CmpConstants.ID_COLUMN, ModelType.STRING, true);
        SimpleAttributeDefinition SELECT_HI_DDL = new SimpleAttributeDefinition(CmpConstants.SELECT_HI_DDL, ModelType.STRING, true);
        SimpleAttributeDefinition SEQUENCE_COLUMN = new SimpleAttributeDefinition(CmpConstants.SEQUENCE_COLUMN, ModelType.STRING, true);
        SimpleAttributeDefinition SEQUENCE_NAME = new SimpleAttributeDefinition(CmpConstants.SEQUENCE_NAME, ModelType.STRING, true);
        SimpleAttributeDefinition TABLE_NAME = new SimpleAttributeDefinition(CmpConstants.TABLE_NAME, ModelType.STRING, true);

        AttributeDefinition[] HILO_ATTRIBUTES = new AttributeDefinition[]{
                BLOCK_SIZE, CREATE_TABLE, CREATE_TABLE_DDL, DATA_SOURCE, DROP_TABLE, ID_COLUMN, SELECT_HI_DDL, SEQUENCE_COLUMN, SEQUENCE_NAME, TABLE_NAME
        };
    }
}
