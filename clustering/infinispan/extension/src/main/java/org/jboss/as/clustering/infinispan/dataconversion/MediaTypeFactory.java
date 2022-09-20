/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.dataconversion;

import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.infinispan.commons.dataconversion.MediaType;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.modules.Module;

/**
 * Factory for key/value media types for a given {@link ClassLoader}.
 * @author Paul Ferraro
 */
public enum MediaTypeFactory implements Function<ClassLoader, Map.Entry<MediaType, MediaType>> {
    INSTANCE;

    private static final String MEDIA_TYPE_PATTERN = "application/%s; type=%s";
    private static final Map.Entry<MediaType, MediaType> DEFAULT_MEDIA_TYPES = Map.entry(MediaType.APPLICATION_OBJECT, MediaType.APPLICATION_OBJECT);

    @Override
    public Map.Entry<MediaType, MediaType> apply(ClassLoader loader) {
        Module module = Module.forClassLoader(loader, false);
        if ((module == null) || !module.getName().startsWith(ServiceModuleLoader.MODULE_PREFIX)) {
            return DEFAULT_MEDIA_TYPES;
        }
        // Generate key/value media types for this module
        String moduleName = module.getName().replaceAll("/", "|");
        MediaType keyMediaType = MediaType.fromString(String.format(Locale.ROOT, MEDIA_TYPE_PATTERN, moduleName, "key"));
        MediaType valueMediaType = MediaType.fromString(String.format(Locale.ROOT, MEDIA_TYPE_PATTERN, moduleName, "value"));
        return Map.entry(keyMediaType, valueMediaType);
    }
}
