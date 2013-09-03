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

import org.jboss.as.controller.access.Authorizer;

/**
 * Hook to expose JMX-related access control configuration to the JMX subsystem without
 * exposing unrelated capabilities.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public interface JmxAuthorizer extends Authorizer {

    /**
     * Gets whether JMX calls to non-facade mbeans (i.e. those that result in invocations to
     * {@link Authorizer#authorizeJmxOperation(org.jboss.as.controller.access.Caller, org.jboss.as.controller.access.Environment,
     * org.jboss.as.controller.access.JmxAction)}) should be treated as 'sensitive'.
     *
     * @param sensitive {@code true} if non-facade mbean calls are sensitive; {@code false} otherwise
     */
    void setNonFacadeMBeansSensitive(boolean sensitive);
}
