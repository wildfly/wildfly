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

package org.jboss.as.controller.access.constraint.management;

import java.util.Locale;

import org.jboss.as.controller.access.constraint.ConstraintFactory;
import org.jboss.dmr.ModelNode;

/**
 * Definition of a constraint that can be associated with a
 * {@link org.jboss.as.controller.ResourceDefinition}, {@link org.jboss.as.controller.OperationDefinition}
 * or {@link org.jboss.as.controller.AttributeDefinition}.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public interface AccessConstraintDefinition {

    /**
     * Get the name of the constraint
     *
     * @return the name
     */
    String getName();

    /**
     * Get the type of constraint
     *
     * @return the type of constraint
     */
    Type getType();

    /**
     * Get descriptive information about the constraint for inclusion in the reaa-xxx-description metadata
     * TODO flesh this out.
     *
     * @param locale locale to use for internationalized text
     * @return
     */
    ModelNode getModelDescription(Locale locale);

    /**
     * Get the factory to use for creating a {@link org.jboss.as.controller.access.constraint.Constraint} that
     * implements
     * @return the factory. Cannot return {@code null}
     */
    ConstraintFactory getConstraintFactory();

    enum Type {
        SENSITIVE,
        APPLICATION;
    }
}
