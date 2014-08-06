/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.session;

import java.util.ServiceLoader;

import org.jboss.msc.value.Value;
import org.wildfly.clustering.ee.Batch;

/**
 * Uses a service loader to load the web session clustering service provider.
 * @author Paul Ferraro
 */
public class SessionManagerFactoryBuilderValue implements Value<SessionManagerFactoryBuilder<Batch>> {
    private final SessionManagerFactoryBuilder<Batch> builder;

    public SessionManagerFactoryBuilderValue() {
        this(load());
    }

    public SessionManagerFactoryBuilderValue(SessionManagerFactoryBuilder<Batch> builder) {
        this.builder = builder;
    }

    private static SessionManagerFactoryBuilder<Batch> load() {
        for (SessionManagerFactoryBuilder<Batch> builder: ServiceLoader.load(SessionManagerFactoryBuilder.class, SessionManagerFactoryBuilder.class.getClassLoader())) {
            return builder;
        }
        return null;
    }

    @Override
    public SessionManagerFactoryBuilder<Batch> getValue() {
        return this.builder;
    }
}
