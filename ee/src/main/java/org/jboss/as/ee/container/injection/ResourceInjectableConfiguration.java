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

package org.jboss.as.ee.container.injection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Abstract class used to allow sub-classes uniform access to allow for resource injection.
 *
 * @author John Bailey
 */
public class ResourceInjectableConfiguration {
    private final List<ResourceInjectionConfiguration> resourceInjections = new ArrayList<ResourceInjectionConfiguration>();

    /**
     * The configurations for any resource injections for this bean type.
     *
     * @return The resource injection configurations
     */
    public List<ResourceInjectionConfiguration> getResourceInjectionConfigs() {
        return Collections.unmodifiableList(resourceInjections);
    }

    /**
     * Add a resource injection configuration.
     *
     * @param injectionConfiguration The injection configuration
     */
    public void addResourceInjectionConfig(final ResourceInjectionConfiguration injectionConfiguration) {
        resourceInjections.add(injectionConfiguration);
    }

    /**
     * Add resource injection configurations.
     *
     * @param injectionConfigurations The injection configurations
     */
    public void addResourceInjectionConfigs(final ResourceInjectionConfiguration... injectionConfigurations) {
        for (ResourceInjectionConfiguration configuration : injectionConfigurations) {
            addResourceInjectionConfig(configuration);
        }
    }

    /**
     * Add resource injection configurations.
     *
     * @param injectionConfigurations The injection configurations
     */
    public void addResourceInjectionConfigs(final Collection<ResourceInjectionConfiguration> injectionConfigurations) {
        resourceInjections.addAll(injectionConfigurations);
    }
}
