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

import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;

/**
 * A description resolver for the batch subsystem.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class BatchResourceDescriptionResolver {
    private static final String RESOURCE_NAME = BatchResourceDescriptionResolver.class.getPackage().getName() + ".LocalDescriptions";
    private static final String BASE = "batch.jberet";

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
