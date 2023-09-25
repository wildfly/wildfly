/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.epcpropagation;

import java.util.Locale;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Local;
import jakarta.ejb.Remove;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import javax.naming.NamingException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
@Local(StatefulInterface.class)
public class NoTxEPCStatefulBean extends AbstractStatefulInterface {
    @PersistenceContext(type = PersistenceContextType.EXTENDED, unitName = "mypc")
    EntityManager em;

    @Resource
    SessionContext sessionContext;

    @EJB
    StatelessInterface cmtBean;

    public boolean execute(Integer id, String name) throws Exception {
        MyEntity eb = em.find(MyEntity.class, id);
        eb.setName(name.toUpperCase(Locale.ENGLISH));

        String propagatedName = cmtBean.updateEntity(id, name.toLowerCase(Locale.ENGLISH));

        return propagatedName.equals(name.toUpperCase(Locale.ENGLISH));
    }


    public boolean createEntity(Integer id, String name) throws Exception {
        boolean result = true;

        cmtBean.createEntity(id, name);  // entity is created in XPC (will not be persisted to DB)

        // first test is that created entity propagated to stateful_2ndSFSBInvocation that inherits XPC from calling SFSB
        StatefulInterface stateful_2ndSFSBInvocation = lookup("NoTxEPCStatefulBean", StatefulInterface.class);
        stateful_2ndSFSBInvocation.execute(8, "EntityName");
        // NPE Exception will occur if entity isn't found.  success is making it the next line

        // repeat same test once more
        StatefulInterface stateful_3rdSFSBInvocation = lookup("NoTxEPCStatefulBean", StatefulInterface.class);
        stateful_3rdSFSBInvocation.execute(8, "EntityName");
        // NPE Exception will occur if entity isn't found.  success is making it the next line

        stateful_2ndSFSBInvocation.finishUp();
        stateful_3rdSFSBInvocation.finishUp();

        // transaction entity manager should still be able to find/update the entity
        cmtBean.updateEntity(id, name + " and Emma Peel");

        MyEntity eb = em.find(MyEntity.class, id);

        result = (eb != null);      // true if we find the entity in our XPC, false otherwise

        return result;
    }

    public StatefulInterface createSFSBOnInvocation() throws Exception {
        return lookup("NoTxEPCStatefulBean", StatefulInterface.class);
    }

    public StatelessInterface createSLSBOnInvocation() throws Exception {
        return lookup("StatelessBean", StatelessInterface.class);
    }

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(sessionContext.lookup("java:module/" + beanName + "!" + interfaceType.getName()));
    }

    public EntityManager getExtendedPersistenceContext() {
        return em;
    }

    @Remove
    public void finishUp() {
    }


}
