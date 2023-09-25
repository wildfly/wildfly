/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet;

import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;

/**
 * A description resolver for the batch subsystem.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class BatchResourceDescriptionResolver {
    public static final String RESOURCE_NAME = BatchResourceDescriptionResolver.class.getPackage().getName() + ".LocalDescriptions";
    public static final String BASE = "batch.jberet";

    public static StandardResourceDescriptionResolver getResourceDescriptionResolver() {
        return new StandardResourceDescriptionResolver(BASE, RESOURCE_NAME, BatchSubsystemExtension.class.getClassLoader(), true, false);
    }

    public static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        String prefix = BASE + (keyPrefix == null ? "" : "." + keyPrefix);
        return new StandardResourceDescriptionResolver(prefix, RESOURCE_NAME, BatchSubsystemExtension.class.getClassLoader(), true, false);
    }

    public static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... prefixes) {
        final StringBuilder prefix = new StringBuilder(BASE);
        for (String p : prefixes) {
            prefix.append('.').append(p);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, BatchSubsystemExtension.class.getClassLoader(), true, false);
    }
}
