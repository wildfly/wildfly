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

package org.jboss.as.modcluster;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
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
 * The modcluster subsystem description providers.
 *
 * @author Jean-Frederic Clere
 */
class ModClusterSubsystemDescriptions {

    static final String RESOURCE_NAME = ModClusterSubsystemDescriptions.class.getPackage().getName() + ".LocalDescriptions";

    static ModelNode getSubsystemDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();

        node.get(DESCRIPTION).set(bundle.getString("modcluster"));
        node.get(HEAD_COMMENT_ALLOWED).set(true);
        node.get(TAIL_COMMENT_ALLOWED).set(true);
        node.get(NAMESPACE).set(Namespace.MODCLUSTER.getUriString());
        getConfigurationCommonDescription(node.get(ATTRIBUTES, CommonAttributes.MOD_CLUSTER_CONFIG), bundle);

        return node;
    }

    static ModelNode getSubsystemAddDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("modcluster.add"));
        return node;
    }

    static ModelNode getListProxiesDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("list-proxies");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.list-proxies"));
        node.get(REQUEST_PROPERTIES).setEmptyObject();
        node.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("modcluster.proxy-list"));
        node.get(REPLY_PROPERTIES, TYPE).set(ModelType.STRING);
        return node;
    }

    static void AddHostPortDescription(ModelNode node, ResourceBundle bundle) {
        node.get(REQUEST_PROPERTIES, "host", DESCRIPTION).set(bundle.getString("modcluster.proxy-host"));
        node.get(REQUEST_PROPERTIES, "host", TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, "host", REQUIRED).set(true);

        node.get(REQUEST_PROPERTIES, "port",DESCRIPTION).set(bundle.getString("modcluster.proxy-port"));
        node.get(REQUEST_PROPERTIES, "port", TYPE).set(ModelType.INT);
        node.get(REQUEST_PROPERTIES, "port", REQUIRED).set(true);
    }
    static ModelNode getAddProxyDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("add-proxy");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.add-proxy"));

        AddHostPortDescription(node, bundle);

        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    static ModelNode getRemoveProxyDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("remove-proxy");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.remove-proxy"));

        AddHostPortDescription(node, bundle);

        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    static ModelNode getRefreshDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("refresh");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.refresh"));
        node.get(REQUEST_PROPERTIES).setEmptyObject();
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    static ModelNode getResetDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("reset");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.reset"));
        node.get(REQUEST_PROPERTIES).setEmptyObject();
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }
    static ModelNode getEnableDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("enable");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.enable"));
        node.get(REQUEST_PROPERTIES).setEmptyObject();
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    static void AddWaitTimeDescription(ModelNode node, ResourceBundle bundle) {
        node.get(REQUEST_PROPERTIES, "waittime",DESCRIPTION).set(bundle.getString("modcluster.waittime"));
        node.get(REQUEST_PROPERTIES, "waittime", TYPE).set(ModelType.INT);
        node.get(REQUEST_PROPERTIES, "waittime", REQUIRED).set(false);
    }
    static ModelNode getStopDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("stop");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.stop"));
        AddWaitTimeDescription(node, bundle);
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    static ModelNode getDisableDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("disable");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.disable"));
        node.get(REQUEST_PROPERTIES).setEmptyObject();
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    static void AddHostContextDescription(ModelNode node, ResourceBundle bundle) {
        node.get(REQUEST_PROPERTIES, "virtualhost", DESCRIPTION).set(bundle.getString("modcluster.virtualhost"));
        node.get(REQUEST_PROPERTIES, "virtualhost", TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, "virtualhost", REQUIRED).set(true);

        node.get(REQUEST_PROPERTIES, "context",DESCRIPTION).set(bundle.getString("modcluster.context"));
        node.get(REQUEST_PROPERTIES, "context", TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, "context", REQUIRED).set(true);
    }

    static ModelNode getEnableContextDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("enable-context");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.enable-context"));
        AddHostContextDescription(node, bundle);
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    static ModelNode getDisableContextDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("disable-context");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.disable-context"));
        AddHostContextDescription(node, bundle);
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    static ModelNode getStopContextDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("stop-context");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.stop-context"));
        AddHostContextDescription(node, bundle);
        AddWaitTimeDescription(node, bundle);
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

    static ModelNode getConfigurationCommonDescription(final ModelNode node, final ResourceBundle bundle) {

        node.get(TYPE).set(ModelType.OBJECT);
        node.get(DESCRIPTION).set(bundle.getString("modcluster.configuration"));
        node.get(REQUIRED).set(false);

        return node;
    }
}
