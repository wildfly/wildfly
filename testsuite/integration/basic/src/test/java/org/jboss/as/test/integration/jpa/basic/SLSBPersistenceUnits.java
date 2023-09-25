/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.basic;

import java.util.Map;
import jakarta.annotation.Resource;
import jakarta.ejb.EJBContext;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;

/**
 * stateless session bean
 *
 * @author Scott Marlow
 */
@Stateless
@PersistenceUnit(name = "pu1", unitName = "pu1")
@PersistenceUnit(name = "pu2", unitName = "pu2")

public class SLSBPersistenceUnits {

    @Resource
    EJBContext ctx;

    public Map<String, Object> getPU1Info() {
        EntityManagerFactory emf = (EntityManagerFactory) ctx.lookup("pu1");
        return emf.getProperties();
    }

    public Map<String, Object> getPU2Info() {
        EntityManagerFactory emf = (EntityManagerFactory) ctx.lookup("pu2");
        return emf.getProperties();
    }

}
