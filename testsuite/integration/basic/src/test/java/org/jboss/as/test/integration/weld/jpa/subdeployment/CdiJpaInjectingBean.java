/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.jpa.subdeployment;

import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;

public class CdiJpaInjectingBean {

    @Produces
    @QualifyEntityManagerFactory
    @PersistenceUnit(unitName = "cdiPu")
    EntityManagerFactory emf;

    public EntityManagerFactory entityManagerFactory() {
        return emf;
    }


}
