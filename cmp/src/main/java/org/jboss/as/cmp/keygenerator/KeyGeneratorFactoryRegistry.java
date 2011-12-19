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

package org.jboss.as.cmp.keygenerator;

import java.util.HashMap;
import java.util.Map;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;

/**
 * @author John Bailey
 */
public class KeyGeneratorFactoryRegistry implements Service<KeyGeneratorFactoryRegistry> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("cmp", "keygen", "registry");

    private final Map<String, KeyGeneratorFactory> factories = new HashMap<String, KeyGeneratorFactory>();

    public synchronized void start(StartContext context) throws StartException {
    }

    public synchronized void stop(StopContext context) {
        factories.clear();
    }

    public synchronized KeyGeneratorFactoryRegistry getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    void addKeyGeneratorFactory(final String name, final KeyGeneratorFactory factory) {
        factories.put(name, factory);
    }

    void removeKeyGeneratorFactory(final String name) {
        factories.remove(name);
    }

    public KeyGeneratorFactory getFactory(final String name) {
        return factories.get(name);
    }

    public static Injector<KeyGeneratorFactoryRegistry> getRegistryInjector(final String name, final Value<KeyGeneratorFactory> factory) {
        return new Injector<KeyGeneratorFactoryRegistry>() {
            private KeyGeneratorFactoryRegistry registry;

            public synchronized void inject(final KeyGeneratorFactoryRegistry value) throws InjectionException {
                registry = value;
                registry.addKeyGeneratorFactory(name, factory.getValue());
            }

            public synchronized void uninject() {
                registry.removeKeyGeneratorFactory(name);
            }
        };
    }
}
