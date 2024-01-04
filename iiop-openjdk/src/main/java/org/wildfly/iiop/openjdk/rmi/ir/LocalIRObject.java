/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

