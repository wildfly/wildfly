/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.epcpropagation.shallow;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateful;

/**
 * top-level stateful bean that has no extended persistence context of its own
 *
 * @author Scott Marlow
 */
@Stateful
public class TopLevelBean {

    @EJB
    FirstDAO firstDAO;

    @EJB
    SecondDAO secondDAO;

    /**
     * Failure is expected when the SecondAO bean is invoked, which will bring in a
     * separate extended persistence context
     */
    public void referenceTwoDistinctExtendedPersistenceContextsInSameTX_fail() {
        firstDAO.noop();
        secondDAO.noop();
    }

    /**
     * not expected to Fail (created bean should use the same XPC)
     */
    public void induceCreationViaJNDILookup() {
        firstDAO.induceCreationViaJNDILookup();

    }

    /**
     * not expected to Fail (created bean should use the same XPC)
     */
    public void induceCreationViaTwoLevelJNDILookup() {
        firstDAO.induceTwoLevelCreationViaJNDILookup();

    }

}
