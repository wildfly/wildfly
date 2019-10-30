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

package org.jboss.as.connector.subsystems.datasources;

import static org.jboss.as.connector.subsystems.datasources.Constants.STATISTICS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.OverrideDescriptionProvider;
import org.jboss.dmr.ModelNode;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 * @author John Bailey
 */
public class DataSourcesSubsystemProviders {


    public static final String RESOURCE_NAME = DataSourcesSubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";


    public static OverrideDescriptionProvider OVERRIDE_DS_DESC = new OverrideDescriptionProvider() {

        @Override
        public Map<String, ModelNode> getAttributeOverrideDescriptions(Locale locale) {
            return Collections.emptyMap();
        }

        @Override
        public Map<String, ModelNode> getChildTypeOverrideDescriptions(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            Map<String, ModelNode> children = new HashMap<String, ModelNode>();
            ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("statistics"));
            children.put(STATISTICS, node);
            return children;
        }
    };



    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}
