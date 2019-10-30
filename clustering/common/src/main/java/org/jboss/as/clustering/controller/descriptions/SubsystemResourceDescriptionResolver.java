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

package org.jboss.as.clustering.controller.descriptions;

import java.util.Arrays;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;

/**
 * Generates resource descriptions for a given subsystem and exposes a mechanism for generating a {@link ResourceDescriptionResolver} for child resources.
 * @author Paul Ferraro
 */
public class SubsystemResourceDescriptionResolver extends StandardResourceDescriptionResolver {

    private static final String RESOURCE_NAME_PATTERN = "%s.LocalDescriptions";

    public SubsystemResourceDescriptionResolver(String subsystemName, Class<? extends Extension> extensionClass) {
        super(subsystemName, String.format(RESOURCE_NAME_PATTERN, extensionClass.getPackage().getName()), extensionClass.getClassLoader(), true, false);
    }

    public ResourceDescriptionResolver createChildResolver(PathElement... paths) {
        return new ChildResourceDescriptionResolver(this, this.getKeyPrefix(), Arrays.asList(paths));
    }
}
