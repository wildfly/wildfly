/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
  *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.jpa.epcpropagation;

import java.util.Locale;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Remove;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

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