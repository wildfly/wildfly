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

/**
 * Dynamic name mapper implementations for binary capability names.
 * @author Paul Ferraro
 */
public enum BinaryCapabilityNameResolver implements Function<PathAddress, String[]> {
    PARENT_CHILD() {
        @Override
        public String[] apply(PathAddress address) {
            return new String[] { address.getParent().getLastElement().getValue(), address.getLastElement().getValue() };
        }
    },
    GRANDPARENT_PARENT() {
        @Override
        public String[] apply(PathAddress address) {
            PathAddress parent = address.getParent();
            return new String[] { parent.getParent().getLastElement().getValue(), parent.getLastElement().getValue() };
        }
    },
    GRANDPARENT_CHILD() {
        @Override
        public String[] apply(PathAddress address) {
            return new String[] { address.getParent().getParent().getLastElement().getValue(), address.getLastElement().getValue() };
        }
    },
}
