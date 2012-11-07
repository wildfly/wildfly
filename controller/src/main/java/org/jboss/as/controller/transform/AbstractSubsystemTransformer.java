/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.transform;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ResourceDefinition;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public abstract class AbstractSubsystemTransformer extends AbstractResourceModelTransformer implements SubsystemTransformer {

    protected AbstractSubsystemTransformer(final ResourceDefinitionLoader loader) {
        super(loader);
    }

    protected AbstractSubsystemTransformer(final String subsystemName) {
        this(new ResourceDefinitionLoader() {
            @Override
            public ResourceDefinition load(TransformationTarget target) {
                final ModelVersion version = target.getSubsystemVersion(subsystemName);
                return TransformationUtils.loadSubsystemDefinition(subsystemName, version);
            }
        });
    }

    protected AbstractSubsystemTransformer(final ResourceDefinition definition) {
        this(new ResourceDefinitionLoader() {
            @Override
            public ResourceDefinition load(TransformationTarget target) {
                return definition;
            }
        });
    }

}
