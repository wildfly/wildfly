package org.jboss.as.test.integration.jpa.initializeinorder;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * @author Scott Marlow
 */
@Singleton
@Startup
public class MyEjb {

    @PersistenceContext(unitName = "pu1")
    EntityManager em;

    @PersistenceContext(unitName = "pu2")
    EntityManager em2;

    @PostConstruct
    public void postConstruct() {
        InitializeInOrderTestCase.recordInit(MyEjb.class.getSimpleName());
    }

    public boolean hasPersistenceContext() {
        return em != null && em2 != null;
    }
}
