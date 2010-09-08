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

import org.jboss.as.Extension;
import org.jboss.as.model.DomainParser;
import org.jboss.as.model.Element;
import org.jboss.as.model.HostParser;
import org.jboss.as.model.Namespace;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.staxmapper.XMLMapper;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.List;

/**
 * A {@link StandardElementReaderRegistrar} that uses a static list of extensions.
 *
 * @author Brian Stansberry
 */
public class StandardElementReaderRegistrarImpl implements StandardElementReaderRegistrar {

    /**
     * Standard modules that include parsing {@link org.jboss.as.Extension}s.
     */
    private static final List<String> EXTENSION_MODULES = Arrays.asList(new String[] {
            "org.jboss.as.jboss-as-logging",
            "org.jboss.as.jboss-as-threads",
            "org.jboss.as.jboss-as-remoting",
            "org.jboss.as.jboss-as-transactions",
            "org.jboss.as.jboss-as-naming"
    });
    
    
    /* (non-Javadoc)
     * @see org.jboss.as.server.manager.ElementHandlerRegistrar#registerStandardDomainHandlers(org.jboss.staxmapper.XMLMapper)
     */
    @Override
    public synchronized void registerStandardDomainReaders(XMLMapper mapper) throws ModuleLoadException {
        
        for (Namespace ns : Namespace.STANDARD_NAMESPACES) {
            mapper.registerRootElement(new QName(ns.getUriString(), Element.DOMAIN.getLocalName()), DomainParser.getInstance());
        }
        
        registerExtensions(mapper);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.server.manager.ElementHandlerRegistrar#registerStandardHostHandlers(org.jboss.staxmapper.XMLMapper)
     */
    @Override
    public synchronized void registerStandardHostReaders(XMLMapper mapper) throws ModuleLoadException {
        
        for (Namespace ns : Namespace.STANDARD_NAMESPACES) {
            mapper.registerRootElement(new QName(ns.getUriString(), Element.HOST.getLocalName()), HostParser.getInstance());
        }
        
        registerExtensions(mapper);

    }
    
    private static void registerExtensions(XMLMapper mapper) throws ModuleLoadException {
        for (String module : EXTENSION_MODULES) {
            for (Extension extension : Module.loadService(module, Extension.class)) {
                extension.registerElementHandlers(mapper);
            }
        }
    }

}
