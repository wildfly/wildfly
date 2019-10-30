/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

import java.util.function.Function;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;

/**
 * Dynamic name mapper implementations for unary capability names.
 * @author Paul Ferraro
 */
public enum UnaryCapabilityNameResolver implements Function<PathAddress, String[]> {
    DEFAULT() {
        @Override
        public String[] apply(PathAddress address) {
            return new String[] { address.getLastElement().getValue() };
        }
    },
    PARENT() {
        @Override
        public String[] apply(PathAddress address) {
            return new String[] { address.getParent().getLastElement().getValue() };
        }
    },
    GRANDPARENT() {
        @Override
        public String[] apply(PathAddress address) {
            return new String[] { address.getParent().getParent().getLastElement().getValue() };
        }
    },
    LOCAL() {
        @Override
        public String[] apply(PathAddress address) {
            return new String[] { ModelDescriptionConstants.LOCAL };
        }
    },
}
