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
package org.jboss.as.domain.controller.descriptions;

import java.util.Locale;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.DeploymentDescription;
import org.jboss.as.controller.descriptions.common.PathDescription;
import org.jboss.as.controller.descriptions.common.ProfileDescription;
import org.jboss.dmr.ModelNode;

/**
 * {@link org.jboss.as.controller.descriptions.DescriptionProvider} implementations for sub-models that occur
 * in the domain-level model.
 *
 * @author Brian Stansberry
 * @author Emanuel Muckenhuber
 */
public final class DomainDescriptionProviders {



    // Prevent instantiation
    private DomainDescriptionProviders() {}

    /**
     * Provider for the host model root.
     */
    public static final DescriptionProvider ROOT_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            return DomainRootDescription.getDescription(locale);
        }
    };

    public static final DescriptionProvider SYSTEM_PROPERTY_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return DomainRootDescription.getSystemPropertiesDescription(locale);
        }
    };

    public static final DescriptionProvider PROFILE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return ProfileDescription.getProfileWithIncludesDescription(locale);
        }
    };

    public static final DescriptionProvider PATH_DESCRIPTION = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return PathDescription.getNamedPathDescription(locale);
        }
    };

    public static final DescriptionProvider DEPLOYMENT_PROVIDER = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return DeploymentDescription.getDeploymentDescription(locale, false, true, false);
        }
    };

    public static final DescriptionProvider SERVER_GROUP = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return ServerGroupDescription.getServerGroupDescription(locale);
        }
    };

    public static final DescriptionProvider SERVER_GROUP_DEPLOYMENT = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return DeploymentDescription.getDeploymentDescription(locale, true, false, false);
        }
    };

    public static final DescriptionProvider SERVER_GROUP_SYSTEM_PROPERTY_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return ServerGroupDescription.getSystemPropertiesDescription(locale);
        }
    };
}
