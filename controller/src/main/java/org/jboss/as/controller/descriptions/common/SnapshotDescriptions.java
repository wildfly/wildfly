/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.controller.descriptions.common;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DIRECTORY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.operations.common.SnapshotDeleteHandler;
import org.jboss.as.controller.operations.common.SnapshotListHandler;
import org.jboss.as.controller.operations.common.SnapshotTakeHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * ModelDescriptions for Snapshot operations handlers
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class SnapshotDescriptions {

    private static final String RESOURCE_NAME = InterfaceDescription.class.getPackage().getName() + ".LocalDescriptions";

    public static ModelNode getSnapshotTakeModel(Locale locale) {
        ModelNode node = new ModelNode();
        node.get(OP).set(SnapshotTakeHandler.OPERATION_NAME);
        node.get(DESCRIPTION).set(getResourceBundle(locale).getString("snapshot.take.description"));
        node.get(REPLY_PROPERTIES).get(NAME).get(TYPE).set(ModelType.STRING);
        node.get(REPLY_PROPERTIES).get(NAME).get(DESCRIPTION).set(getResourceBundle(locale).getString("snapshot.take.reply.name"));

        return node;
    }

    public static ModelNode getSnapshotListModel(Locale locale) {
        ModelNode node = new ModelNode();
        node.get(OP).set(SnapshotListHandler.OPERATION_NAME);
        node.get(DESCRIPTION).set(getResourceBundle(locale).getString("snapshot.list.description"));
        node.get(REPLY_PROPERTIES).get(DIRECTORY).get(TYPE).set(ModelType.STRING);
        node.get(REPLY_PROPERTIES).get(DIRECTORY).get(DESCRIPTION).set(getResourceBundle(locale).getString("snapshot.list.reply.dir"));
        node.get(REPLY_PROPERTIES).get(NAMES).get(TYPE).set(ModelType.LIST);
        node.get(REPLY_PROPERTIES).get(NAMES).get(VALUE_TYPE).set(ModelType.STRING);
        node.get(REPLY_PROPERTIES).get(NAMES).get(DESCRIPTION).set(getResourceBundle(locale).getString("snapshot.list.reply.names"));

        return node;
    }

    public static ModelNode getSnapshotDeleteModel(Locale locale) {
        ModelNode node = new ModelNode();
        node.get(OP).set(SnapshotDeleteHandler.OPERATION_NAME);
        node.get(DESCRIPTION).set(getResourceBundle(locale).getString("snapshot.delete.description"));
        node.get(REPLY_PROPERTIES).get(NAME).get(TYPE).set(ModelType.STRING);
        node.get(REPLY_PROPERTIES).get(NAME).get(REQUIRED).set(true);
        node.get(REPLY_PROPERTIES).get(NAME).get(DESCRIPTION).set(getResourceBundle(locale).getString("snapshot.delete.name"));

        return node;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}
