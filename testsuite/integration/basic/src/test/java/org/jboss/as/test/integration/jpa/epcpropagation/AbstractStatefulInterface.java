/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.epcpropagation;

import jakarta.persistence.EntityManager;

/**
 * @author Scott Marlow
 */
public abstract class AbstractStatefulInterface implements StatefulInterface {
    public String getPostConstructErrorMessage() throws Exception {
        return null;
    }

    public boolean createEntity(Integer id, String name) throws Exception {
        return true;
    }

    public StatefulInterface createSFSBOnInvocation() throws Exception {
        return null;
    }

    public EntityManager getExtendedPersistenceContext() {
        return null;
    }

    public void finishUp() {

    }
}
