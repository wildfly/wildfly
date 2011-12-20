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
    import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
    import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
    import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
    import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ONLY;
    import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
    import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

    import java.util.Arrays;
    import java.util.Collections;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Locale;
    import java.util.Map;
    import java.util.ResourceBundle;

    import org.jboss.as.controller.descriptions.DescriptionProvider;
    import org.jboss.as.controller.descriptions.OverrideDescriptionProvider;
    import org.jboss.as.controller.registry.ManagementResourceRegistration;
    import org.jboss.dmr.ModelNode;
    import org.jboss.dmr.ModelType;
    import org.jboss.jca.core.spi.statistics.StatisticsPlugin;


    /**
     * {@link DescriptionProvider} or {@link OverrideDescriptionProvider} for a resource whose contents include attributes
     * provided by a list of {@link StatisticsPlugin}s. Use the {@link DescriptionProvider} API if the resource only
     * includes the statistics attributes; use {@link OverrideDescriptionProvider} if the resource is an
     * {@link ManagementResourceRegistration#registerOverrideModel(String, OverrideDescriptionProvider) override resource}
     * that adds resource-specific statistics to a generic resource.
     *
     * @author Brian Stansberry (c) 2011 Red Hat Inc.
     */
    public class StatisticsDescriptionProvider implements DescriptionProvider {

        private final String bundleName;
        private final String resourceDescriptionKey;
        private final List<StatisticsPlugin> plugins;

        /**
         * Constructor for the {@link OverrideDescriptionProvider} case. Internationalization support is not provided.
         *
         * @param plugins the statistics plugins
         */
        public StatisticsDescriptionProvider(final StatisticsPlugin... plugins) {
            this(null, null, plugins);
        }

        /**
         * Constructor for the {@link DescriptionProvider} case.
         *
         * @param bundleName name to pass to {@link ResourceBundle#getBundle(String)}
         * @param resourceDescriptionKey key to use for looking up the resource's description in the bundle
         * @param plugins the statistics plugins
         */
        public StatisticsDescriptionProvider(final String bundleName, final String resourceDescriptionKey, final StatisticsPlugin... plugins) {
            this.bundleName = bundleName;
            this.resourceDescriptionKey = resourceDescriptionKey;
            this.plugins = Arrays.asList(plugins);
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode subsystem = new ModelNode();
            subsystem.get(DESCRIPTION).set(bundle.getString(resourceDescriptionKey));

            ModelNode attrs = subsystem.get(ATTRIBUTES);
            final Map<String, ModelNode> attributeDescriptions = getAttributeOverrideDescriptions(locale);
            for (Map.Entry<String, ModelNode> entry : attributeDescriptions.entrySet()) {
                attrs.get(entry.getKey()).set(entry.getValue());
            }

            subsystem.get(OPERATIONS); // placeholder

            subsystem.get(CHILDREN).setEmptyObject(); // no children

            return subsystem;
        }

        public Map<String, ModelNode> getAttributeOverrideDescriptions(Locale locale) {
            Map<String, ModelNode> attributes = new HashMap<String, ModelNode>();
            for (StatisticsPlugin plugin : plugins) {
                for (String name : plugin.getNames()) {
                    ModelNode node = new ModelNode();
                    node.get(DESCRIPTION).set(plugin.getDescription(name));
                    ModelType modelType = ModelType.STRING;
                    if (plugin.getType(name) == int.class) {
                        modelType = ModelType.INT;
                    }
                    if (plugin.getType(name) == long.class) {
                        modelType = ModelType.LONG;
                    }
                    node.get(TYPE).set(modelType);
                    node.get(REQUIRED).set(false);
                    node.get(READ_ONLY).set(true);
                    attributes.put(name, node);
                }
            }
            return attributes;
        }

        private ResourceBundle getResourceBundle(Locale locale) {
            if (locale == null) {
                locale = Locale.getDefault();
            }
            return ResourceBundle.getBundle(bundleName, locale);
        }
    }
