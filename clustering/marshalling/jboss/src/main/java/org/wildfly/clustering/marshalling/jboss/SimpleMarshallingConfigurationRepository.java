/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.jboss;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.jboss.marshalling.MarshallingConfiguration;

/**
 * Simple {@link MarshallingConfigurationRepository} implementation based on an array of {@link MarshallingConfiguration}s.
 * Marshalling versions, while arbitrary, are sequential by convention; and start at 1, not 0, for purely historical reasons.
 * @author Paul Ferraro
 */
public class SimpleMarshallingConfigurationRepository implements MarshallingConfigurationRepository {

    private final MarshallingConfiguration[] configurations;
    private final int currentVersion;

    /**
     * Create a marshalling configuration repository using the specified enumeration of marshalling configuration suppliers.
     * @param enumClass an enum class
     * @param current the supplier of the current marshalling configuration
     * @param context the context with which to obtain the marshalling configuration
     */
    public <C, E extends Enum<E> & Function<C, MarshallingConfiguration>> SimpleMarshallingConfigurationRepository(Class<E> enumClass, E current, C context) {
        this(current.ordinal() + 1, createConfigurations(enumClass, context));
    }

    private static <C, E extends Enum<E> & Function<C, MarshallingConfiguration>> MarshallingConfiguration[] createConfigurations(Class<E> enumClass, C context) {
        List<Function<C, MarshallingConfiguration>> values = Arrays.asList(enumClass.getEnumConstants());
        MarshallingConfiguration[] configurations = new MarshallingConfiguration[values.size()];
        for (int i = 0; i < configurations.length; ++i) {
            configurations[i] = values.get(i).apply(context);
        }
        return configurations;
    }

    /**
     * Create a marshalling configuration repository using the specified marshalling configurations.  The current version is always the last.
     * @param configurations
     */
    public SimpleMarshallingConfigurationRepository(MarshallingConfiguration... configurations) {
        this(configurations.length, configurations);
    }

    /**
     * Create a marshalling configuration repository using the specified marshalling configurations.
     * @param currentVersion the current version
     * @param configurations the configurations for this repository
     */
    private SimpleMarshallingConfigurationRepository(int currentVersion, MarshallingConfiguration... configurations) {
        this.currentVersion = currentVersion;
        this.configurations = configurations;
    }

    @Override
    public int getCurrentMarshallingVersion() {
        return this.currentVersion;
    }

    @Override
    public MarshallingConfiguration getMarshallingConfiguration(int version) {
        return this.configurations[version - 1];
    }
}
