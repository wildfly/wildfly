/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.iiop.openjdk.rmi.ir;

import org.omg.CORBA.ContainedOperations;

/**
 * Interface of local contained IR objects.
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 * @version $Revision: 81018 $
 */
interface LocalContained
        extends ContainedOperations, LocalIRObject {
    /**
     * Get a reference to the local IR implementation that
     * this Contained object exists in.
     */
    RepositoryImpl getRepository();
}

