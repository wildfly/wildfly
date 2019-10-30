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
package org.jboss.as.test.integration.ejb.container.interceptor.incorrect;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.jboss.logging.Logger;

/**
 * Incorrect interceptor - contains 2 methods annotated with the {@link AroundInvoke}.
 *
 * @author Josef Cacek
 */
public class IncorrectContainerInterceptor {

    private static Logger LOGGER = Logger.getLogger(IncorrectContainerInterceptor.class);

    // Private methods -------------------------------------------------------

    @AroundInvoke
    Object method1(final InvocationContext invocationContext) throws Exception {
        LOGGER.trace("method1 invoked");
        return invocationContext.proceed();
    }

    @AroundInvoke
    Object method2(final InvocationContext invocationContext) throws Exception {
        LOGGER.trace("method2 invoked");
        return invocationContext.proceed();
    }
}
