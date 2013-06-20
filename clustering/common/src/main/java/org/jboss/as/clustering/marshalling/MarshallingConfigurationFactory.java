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
package org.jboss.as.clustering.marshalling;

import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.marshalling.reflect.ReflectiveCreator;
import org.jboss.marshalling.reflect.SunReflectiveCreator;
import org.jboss.modules.ModuleLoader;

/**
 * Boilerplate code for marshalling configuration creation.
 * @author Paul Ferraro
 */
public class MarshallingConfigurationFactory {

    public static MarshallingConfiguration createMarshallingConfiguration(ModuleLoader loader) {
        MarshallingConfiguration config = new MarshallingConfiguration();
        config.setClassResolver(ModularClassResolver.getInstance(loader));
        config.setSerializedCreator(new SunReflectiveCreator());
        config.setExternalizerCreator(new ReflectiveCreator());
        return config;
    }

    private MarshallingConfigurationFactory() {
        // Hide constructor
    }
}
