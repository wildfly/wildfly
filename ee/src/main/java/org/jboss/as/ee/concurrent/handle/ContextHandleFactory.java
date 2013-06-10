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
package org.jboss.as.ee.concurrent.handle;

import javax.enterprise.concurrent.ContextService;
import java.util.Map;

/**
 * The factory responsible for creating the context handles with the current context saved
 *
 * @author Eduardo Martins
 */
public interface ContextHandleFactory {

    /**
     * @param contextService
     * @param contextObjectProperties
     * @return
     * @see org.glassfish.enterprise.concurrent.spi.ContextSetupProvider#saveContext(javax.enterprise.concurrent.ContextService, java.util.Map)
     */
    ContextHandle saveContext(ContextService contextService, Map<String, String> contextObjectProperties);

    /**
     * Retrieves the factory's type
     *
     * @return
     */
    Type getType();


    /**
     * enumeration of context types
     */
    enum Type {

        // note that ordinal() value, i.e. declaration order, is used when ordering context handle factories, so ensure types are declared in correct order.

        ALL_CHAINED,

        CLASS_LOADER,

        SECURITY,

        EJB,

        NAMING_CHAINED,
        NAMING_NAMESPACE_SELECTOR,
        NAMING_WRITABLE_SERVICE_BASED_STORE_OWNER,

        CONCURRENT;
    }


}
