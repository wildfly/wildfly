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

package org.jboss.as.ee.component.injection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jboss.as.server.deployment.SimpleAttachable;

/**
 * Abstract class used to allow sub-classes uniform access to allow for resource injection.
 *
 * @author John Bailey
 */
public class ResourceInjectableConfiguration extends SimpleAttachable {
    private final List<ResourceInjectionConfiguration> resourceInjectionConfigs = new ArrayList<ResourceInjectionConfiguration>();
    private final List<ResourceInjection> resourceInjections = new ArrayList<ResourceInjection>();

    /**
     * The configurations for any resource injections for this bean type.
     *
     * @return The resource injection configurations
     */
    public List<ResourceInjectionConfiguration> getResourceInjectionConfigs() {
        return Collections.unmodifiableList(resourceInjectionConfigs);
    }

    /**
     * Add a resource injection configuration.
     *
     * @param injectionConfiguration The injection configuration
     */
    public void addResourceInjectionConfig(final ResourceInjectionConfiguration injectionConfiguration) {
        resourceInjectionConfigs.add(injectionConfiguration);
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
        resourceInjectionConfigs.addAll(injectionConfigurations);
    }



    /**
     * The configurations for any resource injections for this bean type.
     *
     * @return The resource injections
     */
    public List<ResourceInjection> getResourceInjections() {
        return Collections.unmodifiableList(resourceInjections);
    }

    /**
     * Add a resource injection configuration.
     *
     * @param injection The injection configuration
     */
    public void addResourceInjection(final ResourceInjection injection) {
        resourceInjections.add(injection);
    }

    /**
     * Add resource injection configurations.
     *
     * @param injections The injection configurations
     */
    public void addResourceInjectionConfigs(final ResourceInjection... injections) {
        for (ResourceInjection injection : injections) {
            addResourceInjection(injection);
        }
    }

    /**
     * Add resource injection configurations.
     *
     * @param injections The injection
     */
    public void addResourceInjections(final Collection<ResourceInjection> injections) {
        resourceInjections.addAll(injections);
    }
}
