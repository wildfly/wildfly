/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.controller.operations.validation;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Performs validation on detyped operation parameters.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface ParameterValidator {

    /**
     * Validate the parameter with the given name.
     *
     * @param parameterName the name of the parameter. Cannot be {@code null}
     * @param value the parameter value. Cannot be {@code null}
     *
     * @throws OperationFailedException if the value is not valid
     */
    void validateParameter(String parameterName, ModelNode value) throws OperationFailedException;

    /**
     * Validate the parameter with the given name, after first
     * {@link ModelNode#resolve() resolving} the given {@code value}.
     *
     * @param parameterName the name of the parameter. Cannot be {@code null}
     * @param value the parameter value. Cannot be {@code null}
     *
     * @throws OperationFailedException if the value is not valid
     */
    void validateResolvedParameter(String parameterName, ModelNode value) throws OperationFailedException;
}
