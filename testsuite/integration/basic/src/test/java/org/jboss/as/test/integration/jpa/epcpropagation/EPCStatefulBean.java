/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.epcpropagation;

import java.util.Locale;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Local;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;
import jakarta.transaction.UserTransaction;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
@Local
public class EPCStatefulBean extends AbstractStatefulInterface implements StatefulInterface {

    @PersistenceContext(type = PersistenceContextType.EXTENDED, unitName = "mypc")
    EntityManager em;

    @Resource
    SessionContext sessionContext;

    @EJB
    StatelessInterface cmtBean;

    private String constructError = null;

    @PostConstruct
    public void init() {
        try {
            em.createQuery("select f from MyEntity f");

        } catch (Exception e) {
            constructError = e.getMessage();
        }
    }

    public boolean execute(Integer id, String name) throws Exception {
        try {
            UserTransaction tx1 = sessionContext.getUserTransaction();
            tx1.begin();
            em.joinTransaction();
            MyEntity entity = em.find(MyEntity.class, id);
            entity.setName(name.toUpperCase(Locale.ENGLISH));

            String propagatedName = cmtBean.updateEntity(id, name.toLowerCase(Locale.ENGLISH));
            tx1.commit();

            return propagatedName.equals(name.toUpperCase(Locale.ENGLISH));
        } catch (Exception e) {
            try {
                sessionContext.getUserTransaction().rollback();
            } catch (Exception e1) {
                System.out.println("ROLLBACK: " + e1);
            }
            throw e;
        }
    }

    public String getPostConstructErrorMessage() throws Exception {
        return constructError;
    }

    @Override
    public boolean createEntity(Integer id, String name) throws Exception {
        return false;
    }

    @Override
    public StatefulInterface createSFSBOnInvocation() throws Exception {
        return null;
    }


}
