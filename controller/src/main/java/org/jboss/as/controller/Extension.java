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

import org.jboss.as.controller.parsing.ExtensionParsingContext;

/**
 * An extension to the JBoss Application Server.  Implementations of this interface should
 * have a zero-arg constructor.  Extension modules must contain a {@code META-INF/services/org.jboss.as.controller.Extension}
 * file with a line containing the name of the implementation class.
 *
 * @see java.util.ServiceLoader
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface Extension {

    /**
     * Initialize this extension by registering its operation handlers and configuration
     * marshaller with the given {@link ExtensionContext}.
     * <p>When this method is invoked the {@link Thread#getContextClassLoader() thread context classloader} will
     * be set to be the defining class loader of the class that implements this interface.</p>
     *
     * @param context the extension context
     */
    void initialize(ExtensionContext context);

    /**
     * Initialize the XML parsers for this extension and register them with the given {@link ExtensionParsingContext}.
     * <p>When this method is invoked the {@link Thread#getContextClassLoader() thread context classloader} will
     * be set to be the defining class loader of the class that implements this interface.</p>
     *
     * @param context the extension parsing context
     */
    void initializeParsers(ExtensionParsingContext context);
}
