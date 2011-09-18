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

package org.jboss.as.controller.descriptions;

import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;

/**
 * Factory for a {@link DescriptionProvider}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface DescriptionProviderFactory {

    /**
     * Gets a {@link DescriptionProvider} for the given resource.
     *
     * @param resourceRegistration  the resource. Cannot be {@code null}
     * @return  the description provider. Will not be {@code null}
     */
    DescriptionProvider getDescriptionProvider(ImmutableManagementResourceRegistration resourceRegistration);

    /** A default implementation of this interface */
    class DefaultFactory implements DescriptionProviderFactory {
        public static DescriptionProviderFactory create(final String keyPrefix,
                                                        final String bundleBaseName,
                                                        final ClassLoader bundleLoader) {
            StandardResourceDescriptionResolver resolver =
                    new StandardResourceDescriptionResolver(keyPrefix, bundleBaseName, bundleLoader);
            return  new DefaultFactory(resolver);
        }

        public static DescriptionProviderFactory create(ResourceDescriptionResolver descriptionResolver) {
            return  new DefaultFactory(descriptionResolver);
        }

        final ResourceDescriptionResolver descriptionResolver;
        private DefaultFactory(final ResourceDescriptionResolver descriptionResolver) {
            this.descriptionResolver = descriptionResolver;
        }

        @Override
        public DescriptionProvider getDescriptionProvider(ImmutableManagementResourceRegistration resourceRegistration) {
            return new DefaultResourceDescriptionProvider(resourceRegistration, descriptionResolver);
        }
    }
}
