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
package org.jboss.as.domain.management.access;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_CONSTRAINT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.registry.Resource;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RootAccessConstraintResourceDefinition extends SimpleResourceDefinition {

    public static PathElement PATH_ELEMENT = PathElement.pathElement(CORE_SERVICE, ACCESS_CONSTRAINT);

    public RootAccessConstraintResourceDefinition() {
        //TODO use proper description resolver
        super(PATH_ELEMENT, new NonResolvingResourceDescriptionResolver());
    }

    private static class SensitivityClasssificationResource extends AbstractClassificationResource {

        private static final Set<String> CHILD_TYPES;
        static {
            Set<String> set = new HashSet<>();
            set.add(SensitivityTypeResourceDefinition.PATH_ELEMENT.getKey());
            CHILD_TYPES = Collections.unmodifiableSet(set);
        }

        public SensitivityClasssificationResource() {
            super(PATH_ELEMENT);
        }

        @Override
        public Set<String> getChildTypes() {
            return CHILD_TYPES;
        }

        @Override
        public Resource getChild(PathElement element) {
            return null;
        }

        @Override
        ResourceEntry getChildEntry(String type, String name) {
            return null;
        }

        @Override
        public Set<String> getChildrenNames(String type) {
            return null;
        }

        @Override
        public Set<ResourceEntry> getChildren(String childType) {
            return null;
        }

    }

}
