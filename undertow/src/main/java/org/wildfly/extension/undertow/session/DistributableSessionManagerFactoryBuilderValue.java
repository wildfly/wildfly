/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.undertow.session;

import java.util.ServiceLoader;

import org.jboss.msc.value.Value;

/**
 * Uses a service loader to load a {@link DistributableSessionManagerFactoryBuilder} implementation.
 * This serves to decouple the undertow subsystem from the clustering modules.
 * @author Paul Ferraro
 */
public class DistributableSessionManagerFactoryBuilderValue implements Value<DistributableSessionManagerFactoryBuilder> {

    private final DistributableSessionManagerFactoryBuilder builder;

    public DistributableSessionManagerFactoryBuilderValue() {
        this(load());
    }

    public DistributableSessionManagerFactoryBuilderValue(DistributableSessionManagerFactoryBuilder builder) {
        this.builder = builder;
    }

    private static DistributableSessionManagerFactoryBuilder load() {
        for (DistributableSessionManagerFactoryBuilder builder: ServiceLoader.load(DistributableSessionManagerFactoryBuilder.class, DistributableSessionManagerFactoryBuilder.class.getClassLoader())) {
            return builder;
        }
        return null;
    }

    @Override
    public DistributableSessionManagerFactoryBuilder getValue() {
        return this.builder;
    }
}
