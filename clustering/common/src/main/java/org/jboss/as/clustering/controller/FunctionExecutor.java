/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

import org.wildfly.common.function.ExceptionFunction;

/**
 * Encapsulates execution of a function.
 * @author Paul Ferraro
 * @param <T> the type of the function argument
 */
public interface FunctionExecutor<T> {
    /**
     * Executes the given function.
     * @param <R> the return type
     * @param <E> the exception type
     * @param function a function to execute
     * @return the result of the function
     * @throws E if the function fails to execute
     */
    <R, E extends Exception> R execute(ExceptionFunction<T, R, E> function) throws E;
}
