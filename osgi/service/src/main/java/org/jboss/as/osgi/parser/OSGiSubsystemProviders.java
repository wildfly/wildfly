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
package org.jboss.as.osgi.parser;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author David Bosschaert
 */
class OSGiSubsystemProviders {
    static final String RESOURCE_NAME = OSGiSubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {
        public ModelNode getModelDescription(final Locale locale) {
            return getRootResource(locale);
        }
    };

    static final DescriptionProvider OSGI_CONFIGURATION_RESOURCE = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return getCasConfiguration(locale);
        }
    };


    static final DescriptionProvider OSGI_PROPERTY_RESOURCE = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return getProperty(locale);
        }
    };


    static final DescriptionProvider OSGI_MODULE_RESOURCE = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return getModule(locale);
        }
    };

    static ModelNode getRootResource(Locale locale) {
        ResourceBundle bundle = OSGiSubsystemProviders.getResourceBundle(locale);

        ModelNode node = new ModelNode();
        node.get(ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("osgi"));
        OSGiSubsystemAdd.addModelProperties(bundle, node, ModelDescriptionConstants.ATTRIBUTES);

        node.get(ModelDescriptionConstants.CHILDREN, CommonAttributes.CONFIGURATION).set(getCasConfiguration(locale));
        node.get(ModelDescriptionConstants.CHILDREN, CommonAttributes.PROPERTY).set(getProperty(locale));
        node.get(ModelDescriptionConstants.CHILDREN, CommonAttributes.MODULE).set(getModule(locale));
        return node;
    }

    private static ModelNode getCasConfiguration(Locale locale) {
        ResourceBundle bundle = OSGiSubsystemProviders.getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("config"));
        OSGiCasConfigAdd.addModelProperties(bundle, node, ModelDescriptionConstants.ATTRIBUTES);

        return node;
    }

    private static ModelNode getProperty(Locale locale) {
        ResourceBundle bundle = OSGiSubsystemProviders.getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("property"));
        OSGiPropertyAdd.addModelProperties(bundle, node, ModelDescriptionConstants.ATTRIBUTES);

        return node;
    }

    private static ModelNode getModule(Locale locale) {
        ResourceBundle bundle = OSGiSubsystemProviders.getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("module"));
        OSGiModuleAdd.addModelProperties(bundle, node, ModelDescriptionConstants.ATTRIBUTES);

        return node;
    }

    static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}
