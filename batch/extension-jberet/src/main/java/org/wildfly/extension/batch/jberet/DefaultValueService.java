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

package org.wildfly.extension.batch.jberet;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A simple service for storing a default value from another service.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class DefaultValueService<T> implements Service<T> {

    public static <T> DefaultValueService<T> create() {
        return new DefaultValueService<>();
    }

    private final InjectedValue<T> injector = new InjectedValue<>();

    private volatile T instance;

    @Override
    public void start(final StartContext context) throws StartException {
        instance = injector.getValue();
    }

    @Override
    public void stop(final StopContext context) {
        instance = null;
    }

    @Override
    public T getValue() throws IllegalStateException, IllegalArgumentException {
        return instance;
    }

    protected InjectedValue<T> getInjector() {
        return injector;
    }
}
