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

package com.redhat.gss.extension;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

public class RedhatAccessPluginEapDescriptions {

    private static final String RESOURCE_NAME = RedhatAccessPluginEapDescriptions.class.getPackage().getName() + ".LocalDescriptions";

    public static ModelNode RedhatAccessPluginEapDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode result = new ModelNode();
        result.get(DESCRIPTION).set(bundle.getString("redhat-access-plugin-eap.subsystem"));
        return result;
    }

    public static ModelNode getSubsystemAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode result = new ModelNode();
        result.get(OPERATION_NAME).set(RedhatAccessPluginEapSubsystemAdd.OPERATION_NAME);
        result.get(DESCRIPTION).set(bundle.getString("redhat-access-plugin-eap.add"));
        result.get(ATTRIBUTES, NAME, DESCRIPTION).set("username");
        result.get(ATTRIBUTES, NAME, REQUIRED).set(true);
        result.get(ATTRIBUTES, PATH, TYPE).set(ModelType.STRING);
        result.get(REQUEST_PROPERTIES).setEmptyObject();
        result.get(REPLY_PROPERTIES).setEmptyObject();

        return result;
    }

    public static ModelNode getSubsystemRemove(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode result = new ModelNode();
        result.get(OPERATION_NAME).set(RedhatAccessPluginEapSubsystemRemove.OPERATION_NAME);
        result.get(DESCRIPTION).set(bundle.getString("redhat-access-plugin-eap.remove"));
        result.get(REQUEST_PROPERTIES).setEmptyObject();
        result.get(REPLY_PROPERTIES).setEmptyObject();

        return result;
    }

    public static ModelNode getRedhatAccessPluginEapRequestDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode result = new ModelNode();
        result.get(OPERATION_NAME).set("Search");
        result.get(DESCRIPTION).set("Search");
        result.get(ATTRIBUTES, "username", DESCRIPTION).set("username");
        result.get(ATTRIBUTES, "username", REQUIRED).set(true);
        result.get(ATTRIBUTES, "password", DESCRIPTION).set("password");
        result.get(ATTRIBUTES, "password", REQUIRED).set(true);
        return result;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}
