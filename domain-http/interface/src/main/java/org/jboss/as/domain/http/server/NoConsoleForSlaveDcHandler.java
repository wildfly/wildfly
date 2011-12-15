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

package org.jboss.as.domain.http.server;

import org.jboss.modules.ModuleLoadException;

/**
 * A simple handler to serve up error pages if attempting to connect to the console on a slave dc
 *
 * @author Kabir Khan
 */
class NoConsoleForSlaveDcHandler extends ResourceHandler {

    private static final String ERROR_MODULE = "org.jboss.as.domain-http-error-context";
    private static final String CONTEXT = "/console";
    private static final String DEFAULT_RESOURCE = "/slaveDcConsoleError.html";

    NoConsoleForSlaveDcHandler() throws ModuleLoadException {
        super(CONTEXT, DEFAULT_RESOURCE, getClassLoader(ERROR_MODULE));
    }

    @Override
    protected boolean skipCache(String resource) {
        /*
         * This context is not expected to be used a lot, however if the pages can
         * be cached this can cause problems with new installations that may
         * have different content.
         */
        return true;
    }



}
