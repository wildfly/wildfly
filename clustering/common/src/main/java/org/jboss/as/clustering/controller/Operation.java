/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
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
package org.jboss.as.clustering.controller;

import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Interface to be implemented by operation enumerations.
 *
 * @param <C> operation context
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public interface Operation<C> extends Definable<OperationDefinition> {

    default String getName() {
        return this.getDefinition().getName();
    }

    /**
     * Execute against the specified context.
     *
     * @param expressionResolver an expression resolver
     * @param operation original operation model to resolve parameters from
     * @param context an execution context
     * @return the execution result (possibly null).
     */
    ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, C context) throws OperationFailedException;
}
