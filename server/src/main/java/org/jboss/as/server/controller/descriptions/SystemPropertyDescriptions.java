/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.server.controller.descriptions;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BOOT_TIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.server.operations.SystemPropertyAddHandler;
import org.jboss.as.server.operations.SystemPropertyRemoveHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SystemPropertyDescriptions {

    private static ResourceBundle getResourceBundle(Locale locale) {
        return ServerDescriptions.getResourceBundle(locale);
    }

    public static ModelNode getSystemPropertyDescription(final Locale locale, final String description, final boolean useBoottime) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(description);
        root.get(HEAD_COMMENT_ALLOWED).set(true);
        root.get(TAIL_COMMENT_ALLOWED).set(false);
        root.get(ATTRIBUTES, VALUE, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, VALUE, DESCRIPTION).set(bundle.getString("system-property.value"));
        root.get(ATTRIBUTES, VALUE, REQUIRED).set(false);
        if (useBoottime) {
            root.get(ATTRIBUTES, BOOT_TIME, TYPE).set(ModelType.BOOLEAN);
            root.get(ATTRIBUTES, BOOT_TIME, DESCRIPTION).set(bundle.getString("system-property.boottime"));
            root.get(ATTRIBUTES, BOOT_TIME, REQUIRED).set(false);
            root.get(ATTRIBUTES, BOOT_TIME, DEFAULT).set(true);
        }
        return root;
    }

    public static ModelNode getAddSystemPropertyOperation(final Locale locale, final boolean useBoottime) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(SystemPropertyAddHandler.OPERATION_NAME);
        root.get(DESCRIPTION).set(bundle.getString("system-property.add"));
        root.get(REQUEST_PROPERTIES, VALUE, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, VALUE, DESCRIPTION).set(bundle.getString("system-property.value"));
        root.get(REQUEST_PROPERTIES, VALUE, REQUIRED).set(false);
        root.get(REQUEST_PROPERTIES, VALUE, NILLABLE).set(true);
        if (useBoottime) {
            root.get(REQUEST_PROPERTIES, BOOT_TIME, TYPE).set(ModelType.BOOLEAN);
            root.get(REQUEST_PROPERTIES, BOOT_TIME, DESCRIPTION).set(bundle.getString("system-property.boottime"));
            root.get(REQUEST_PROPERTIES, BOOT_TIME, REQUIRED).set(false);
            root.get(REQUEST_PROPERTIES, BOOT_TIME, NILLABLE).set(true);
            root.get(REQUEST_PROPERTIES, BOOT_TIME, DEFAULT).set(true);
        }
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static ModelNode getRemoveSystemPropertyOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(SystemPropertyRemoveHandler.OPERATION_NAME);
        root.get(DESCRIPTION).set(bundle.getString("system-property.remove"));
        root.get(REQUEST_PROPERTIES).setEmptyObject();
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }
}
