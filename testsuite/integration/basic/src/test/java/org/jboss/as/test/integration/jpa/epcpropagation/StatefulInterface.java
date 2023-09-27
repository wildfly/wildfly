/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.epcpropagation;

import jakarta.ejb.Local;
import jakarta.persistence.EntityManager;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@Local
public interface StatefulInterface {
    boolean execute(Integer id, String name) throws Exception;

    String getPostConstructErrorMessage() throws Exception;

    /**
     * @param id
     * @param name
     * @return true for success
     * @throws Exception
     */
    boolean createEntity(Integer id, String name) throws Exception;

    StatefulInterface createSFSBOnInvocation() throws Exception;

    EntityManager getExtendedPersistenceContext();

    void finishUp() throws Exception;
}
