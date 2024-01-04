/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.basic;

import java.util.Map;
import jakarta.annotation.Resource;
import jakarta.ejb.EJBContext;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContexts;

/**
 * stateless session bean
 *
 * @author Scott Marlow
 */
@Stateless
@PersistenceContexts({
        @PersistenceContext(name = "pu1", unitName = "pu1"),
        @PersistenceContext(name = "pu2", unitName = "pu2")
})
public class SLSBPersistenceContexts {

    @Resource
    EJBContext ctx;

    public Map<String, Object> getPU1Info() {
        EntityManager em = (EntityManager) ctx.lookup("pu1");
        return em.getEntityManagerFactory().getProperties();
    }

    public Map<String, Object> getPU2Info() {
        EntityManager em = (EntityManager) ctx.lookup("pu2");
        return em.getEntityManagerFactory().getProperties();
    }

}
