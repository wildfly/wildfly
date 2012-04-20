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

    import org.jboss.as.connector.subsystems.resourceadapters.Constants;
    import org.jboss.as.controller.descriptions.DescriptionProvider;
    import org.jboss.dmr.ModelNode;

    import java.util.Locale;
    import java.util.ResourceBundle;

    import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
    import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
    import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;


    /**
     *
     * @author Stefano Maestri (c) 2011 Red Hat Inc.
     */
    public class StatisticsElementDescriptionProvider implements DescriptionProvider {

        private final String bundleName;
        private final String resourceDescriptionKey;



        /**
         * Constructor for the {@link org.jboss.as.controller.descriptions.DescriptionProvider} case.
         *
         * @param bundleName name to pass to {@link java.util.ResourceBundle#getBundle(String)}
         * @param resourceDescriptionKey key to use for looking up the resource's description in the bundle
         *
         */
        public StatisticsElementDescriptionProvider(final String bundleName, final String resourceDescriptionKey) {
            this.bundleName = bundleName;
            this.resourceDescriptionKey = resourceDescriptionKey;

        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode subsystem = new ModelNode();
            subsystem.get(DESCRIPTION).set(bundle.getString(resourceDescriptionKey));

            subsystem.get(OPERATIONS); // placeholder

            subsystem.get(CHILDREN, Constants.CONNECTIONDEFINITIONS_NAME, DESCRIPTION).set(bundle.getString(Constants.CONNECTIONDEFINITIONS_NAME));


            return subsystem;
        }


        private ResourceBundle getResourceBundle(Locale locale) {
            if (locale == null) {
                locale = Locale.getDefault();
            }
            return ResourceBundle.getBundle(bundleName, locale);
        }
    }
