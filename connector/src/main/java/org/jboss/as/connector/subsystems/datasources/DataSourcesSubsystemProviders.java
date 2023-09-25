/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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


    public static final OverrideDescriptionProvider OVERRIDE_DS_DESC = new OverrideDescriptionProvider() {

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
