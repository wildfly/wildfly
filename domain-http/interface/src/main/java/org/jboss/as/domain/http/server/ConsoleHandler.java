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

import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

/**
 * An extension of the ResourceHandler to configure the handler to server up resources
 * from the console module only.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ConsoleHandler extends ResourceHandler {

    private static final String CONSOLE_MODULE = "org.jboss.as.console";
    private static final String CONTEXT = "/console";
    private static final String DEFAULT_RESOURCE = "/index.html";

    public ConsoleHandler() throws ModuleLoadException {
        super(CONTEXT, DEFAULT_RESOURCE, getClassLoader());
    }

    private static ClassLoader getClassLoader() throws ModuleLoadException {
        ModuleIdentifier id = ModuleIdentifier.fromString(CONSOLE_MODULE);
        ClassLoader cl = Module.getCallerModuleLoader().loadModule(id).getClassLoader();

        return cl;
    }

}
