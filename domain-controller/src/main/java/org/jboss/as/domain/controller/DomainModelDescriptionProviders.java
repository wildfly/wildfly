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
package org.jboss.as.domain.controller;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.registry.DescriptionProviderRegistry;
import org.jboss.as.domain.controller.descriptions.DomainRootDescription;
import org.jboss.dmr.ModelNode;

/**
 * Provides the DomainController access to the descriptions of the domain model.
 *
 * @author Brian Stansberry
 */
final class DomainModelDescriptionProviders {

    // Prevent instantiation
    private DomainModelDescriptionProviders() {}

    /**
     * Provides the description of the root node for a domain model.
     */
    private static final ModelDescriptionProvider DOMAIN_ROOT_PROVIDER = new ModelDescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final boolean recursive) {
            return DomainRootDescription.getDescription(recursive);
        }
    };

    /**
     * Registers the providers for the submodels that comprise the domain model.
     *
     * @param registry the registry with which the providers should be registered
     */
    static void registerDomainProviders(final DescriptionProviderRegistry registry) {
        registry.register(PathAddress.EMPTY_ADDRESS, DOMAIN_ROOT_PROVIDER);
        registry.register(PathAddress.pathAddress(PathElement.pathElement("path")), CommonProviders.NAMED_PATH_PROVIDER);
        // FIXME add others as they are created
    }
}
