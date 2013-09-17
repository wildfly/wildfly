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

package org.jboss.as.controller.access.management;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.PathAddress;

/**
 * {@link AccessConstraintUtilization} implementation.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
class AccessConstraintUtilizationImpl implements AccessConstraintUtilization {

    private final AccessConstraintKey constraint;
    private final PathAddress pathAddress;
    private volatile Boolean resourceConstrained;
    private Set<String> attributes = Collections.synchronizedSet(new HashSet<String>());
    private Set<String> operations = Collections.synchronizedSet(new HashSet<String>());

    public AccessConstraintUtilizationImpl(AccessConstraintKey constraint, PathAddress pathAddress) {
        this.constraint = constraint;
        this.pathAddress = pathAddress;
    }

    @Override
    public PathAddress getPathAddress() {
        return pathAddress;
    }

    @Override
    public boolean isEntireResourceConstrained() {
        final Boolean constrained = resourceConstrained;
        return constrained == null ? false : constrained;
    }

    @Override
    public Set<String> getAttributes() {
        return Collections.unmodifiableSet(attributes);
    }

    @Override
    public Set<String> getOperations() {
        return Collections.unmodifiableSet(operations);
    }

    @Override
    public int hashCode() {
        return pathAddress.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null
                && obj instanceof AccessConstraintUtilizationImpl
                && pathAddress.equals(((AccessConstraintUtilizationImpl) obj).pathAddress);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{pathAddress=" + pathAddress + '}';
    }

    void setResourceConstrained(boolean resourceConstrained) {
        this.resourceConstrained = resourceConstrained;
    }

    void addAttribute(String attribute) {
        attributes.add(attribute);
    }

    void addOperation(String operation) {
        operations.add(operation);
    }
}
