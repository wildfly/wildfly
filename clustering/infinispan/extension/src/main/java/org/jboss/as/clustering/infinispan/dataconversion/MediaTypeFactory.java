/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
