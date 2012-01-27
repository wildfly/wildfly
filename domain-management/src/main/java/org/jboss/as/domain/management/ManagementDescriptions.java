/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.domain.management.ModelDescriptionConstants.VERBOSE;
import static org.jboss.as.domain.management.ModelDescriptionConstants.WHOAMI;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Description providers for domain management.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ManagementDescriptions {

    private static final String RESOURCE_NAME = ManagementDescriptions.class.getPackage().getName() + ".LocalDescriptions";

    public static ModelNode getWhoamiOperationDescription(Locale locale) {
        ResourceBundle bundle = getResourceBundle(locale);

        ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(WHOAMI);
        node.get(DESCRIPTION).set(bundle.getString("core.management.whoami"));
        node.get(REQUEST_PROPERTIES, VERBOSE, TYPE).set(ModelType.BOOLEAN);
        node.get(REQUEST_PROPERTIES, VERBOSE, DESCRIPTION).set(bundle.getString("core.management.whoami.verbose"));
        node.get(REQUEST_PROPERTIES, VERBOSE, REQUIRED).set(false);
        node.get(REQUEST_PROPERTIES, VERBOSE, NILLABLE).set(true);
        node.get(REQUEST_PROPERTIES, VERBOSE, DEFAULT).set(false);

        node.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("core.management.whoami.reply"));
        // TODO - Describe the response vales.

        return node;

    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}
