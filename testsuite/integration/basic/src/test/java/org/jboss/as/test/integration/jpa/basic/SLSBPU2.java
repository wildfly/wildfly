/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.basic;

import java.util.Map;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;

/**
 * stateless session bean
 *
 * @author Scott Marlow
 */
@Stateless
public class SLSBPU2 {

    @PersistenceUnit(unitName = "pu2")
    private EntityManagerFactory emf;

    public Map<String, Object> getEMInfo() {
        return emf.getProperties();
    }

}
