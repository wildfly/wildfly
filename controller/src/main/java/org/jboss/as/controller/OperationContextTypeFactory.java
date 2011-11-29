/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller;

/**
* Factory for the currently correct {@link OperationContext.Type} for a new {@link OperationContext}.
*
* @author Brian Stansberry (c) 2011 Red Hat Inc.
*/
public interface OperationContextTypeFactory {

    /**
     * Provides the currently correct type for a new {@link OperationContext}
     *
     * @return the type. Will not return {@code null}
     */
    OperationContext.Type getOperationContextType();

    /** Simple {@code OperationContextTypeFactory} that always returns the type passed to the constructor. */
    class SimpleTypeFactory implements OperationContextTypeFactory {

        private final OperationContext.Type type;

        /**
         * Creates a new {@code SimpleTypeFactory}.
         *
         * @param type the type to provide.
         */
        public SimpleTypeFactory(OperationContext.Type type) {
            this.type = type;
        }

        @Override
        public OperationContext.Type getOperationContextType() {
            return type;
        }
    }
}
