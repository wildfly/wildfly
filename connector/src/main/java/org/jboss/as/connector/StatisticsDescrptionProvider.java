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

package org.jboss.as.connector;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ONLY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.jca.core.spi.statistics.StatisticsPlugin;


/**
 * TODO class javadoc.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class StatisticsDescrptionProvider implements DescriptionProvider {

    static final String RESOURCE_NAME = StatisticsDescrptionProvider.class.getPackage().getName() + ".LocalDescriptions";

    private final StatisticsPlugin plugin;

    public StatisticsDescrptionProvider(final StatisticsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        //final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set("description");
                //bundle.getString("statistics"));

        for (String name : plugin.getNames()) {
            node.get(ATTRIBUTES, name, DESCRIPTION).set(plugin.getDescription(name));
            ModelType modelType = ModelType.STRING;
            if (plugin.getType(name) == int.class) {
                modelType = ModelType.INT;
            }
            if (plugin.getType(name) == long.class) {
                modelType = ModelType.LONG;
            }
            node.get(ATTRIBUTES, name, TYPE).set(modelType);
            node.get(ATTRIBUTES, name, REQUIRED).set(false);
            node.get(ATTRIBUTES, name, READ_ONLY).set(true);
        }
        // Should this be an attribute instead


        return node;
    }

    private ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

}
