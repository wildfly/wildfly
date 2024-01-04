/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.clustering;

import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaData;

/**
 * @author Jaikiran Pai
 * @author Flavia Rainone
 */
public class EJBBoundClusteringMetaData extends AbstractEJBBoundMetaData {

    private static final long serialVersionUID = 4149623336107841341L;
    private boolean singleton;

    public void setClusteredSingleton(boolean singleton) {
        this.singleton = singleton;
    }

    public boolean isClusteredSingleton() {
        return singleton;
    }
}
