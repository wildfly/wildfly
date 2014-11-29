/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.iiop.openjdk.rmi.ir;

import org.omg.CORBA.IRObject;
import org.omg.CORBA.IRObjectOperations;

/**
 * Interface of local IRObject implementations.
 * <p/>
 * This defines the local (non-exported) methods.
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 * @version $Revision: 81018 $
 */
interface LocalIRObject
        extends IRObjectOperations {
    /**
     * Get an exported CORBA reference to this IRObject.
     */
    IRObject getReference();

    /**
     * Finalize the building process, and export.
     */
    void allDone() throws IRConstructionException;

    /**
     * Get a reference to the local IR implementation that
     * this IR object exists in.
     */
    RepositoryImpl getRepository();

    /**
     * Unexport this object.
     */
    void shutdown();
}

