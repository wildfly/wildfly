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

package org.jboss.as.domain.controller.transformers;

import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.as.domain.management.CoreManagementResourceDefinition;

/**
 * Transformers for the domain-wide management configuration.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
class ManagementTransformers {

    /**
     * TODO remove this; it's just temporary to avoid breaking slaves in non-RBAC configs unti
     * propagation of RBAC configs to slaves is sorted.
     * @param parent the parent registration
     */
    static void registerTransformers200(TransformersSubRegistration parent) {
        parent.registerSubResource(CoreManagementResourceDefinition.PATH_ELEMENT, ResourceTransformer.DISCARD);
    }

    private ManagementTransformers() {
        // prevent instantiation
    }
}
