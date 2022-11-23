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

package org.jboss.as.clustering.controller.descriptions;

import java.util.List;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;

/**
 * A factory for creating resource description resolvers for child resources.
 * @author Paul Ferraro
 */
public interface ChildResourceDescriptionResolverFactory {

    default ResourceDescriptionResolver createChildResolver(PathElement path) {
        return this.createChildResolver(List.of(path));
    }

    default ResourceDescriptionResolver createChildResolver(PathElement path1, PathElement path2) {
        return this.createChildResolver(List.of(path1, path2));
    }

    default ResourceDescriptionResolver createChildResolver(PathElement... paths) {
        return this.createChildResolver(List.of(paths));
    }

    ResourceDescriptionResolver createChildResolver(List<PathElement> paths);
}
