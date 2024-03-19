/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.mockprovider.txtimeout;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;

/**
 * EE 11 variant of AbstractTestPersistenceProvider.
 *c
 * @author Brian Stansberry
 */
public class TestPersistenceProvider extends AbstractTestPersistenceProvider {

    @Override
    public EntityManagerFactory createEntityManagerFactory(PersistenceConfiguration configuration) {
        return null;
    }
}
