/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

/**
 * 
 */
package org.jboss.as.domain.controller;

import org.jboss.modules.ModuleLoadException;
import org.jboss.staxmapper.XMLMapper;

/**
 * An object responsible for registering the standard set of {@link org.jboss.staxmapper.XMLElementReader}
 * implementations needed for parsing a domain.xml or a host.xml.
 *
 * @author Brian Stansberry
 */
public interface StandardElementReaderRegistrar {

    void registerStandardHostReaders(XMLMapper mapper) throws ModuleLoadException;

    void registerStandardDomainReaders(XMLMapper mapper) throws ModuleLoadException;

    /**
     * A factory for creating an instance of {@link org.jboss.as.domain.controller.StandardElementReaderRegistrar}.
     */
    class Factory {

        private static StandardElementReaderRegistrar registrar = new StandardElementReaderRegistrarImpl();
        private Factory() {
        }

        /**
         * Gets a StandardElementHandlerRegistrar instance.
         *
         * @return the registrar instance
         */
        public static StandardElementReaderRegistrar getRegistrar() {
            return registrar;
        }
    }
}
