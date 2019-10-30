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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
    SetupContextHandle saveContext(ContextService contextService, Map<String, String> contextObjectProperties);

    /**
     * The factory priority is used to define the order of handles when chained. The handle with the lowest priority is the first in the chain.
     * @return
     */
    int getChainPriority();

    /**
     * Retrieves the factory's name.
     * @return
     */
    String getName();

    /**
     * Writes the handle to the specified output stream.
     * @param contextHandle
     * @param out
     */
    void writeSetupContextHandle(SetupContextHandle contextHandle, ObjectOutputStream out) throws IOException;

    /**
     * Reads a handle from the specified input stream.
     * @param in
     * @return
     */
    SetupContextHandle readSetupContextHandle(ObjectInputStream in) throws IOException, ClassNotFoundException;

}
