/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ejb.entity.cmp.optimisticlock.bug1006723.testsession;


import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Iterator;
import java.util.Date;
import org.jboss.as.test.integration.ejb.entity.cmp.optimisticlock.bug1006723.testentity.EntityALocal;
import org.jboss.as.test.integration.ejb.entity.cmp.optimisticlock.bug1006723.testentity.EntityALocalHome;
import org.jboss.as.test.integration.ejb.entity.cmp.optimisticlock.bug1006723.testentity.EntityBLocal;
import org.jboss.as.test.integration.ejb.entity.cmp.optimisticlock.bug1006723.testentity.EntityBLocalHome;

public class TestSessionBean implements SessionBean {

    public Long setup() throws Exception {
        try {
            /* Add an entity A and then add some entity Bs to it. */
            EntityALocal entityA = getEntityALocalHome().create(new Long(1));
            entityA.addEntityB(getEntityBLocalHome().create(new Long(2)));
            return entityA.getOID();
        } catch (Exception e) {
            sessionContext.setRollbackOnly();
            throw e;
        }
    }

    public void test(Long entityAOID) throws Exception {
        try {
            EntityALocal entityA = getEntityALocalHome().findByPrimaryKey
                    (entityAOID);
            Iterator entityBs = entityA.listEntityBs().iterator();
            while (entityBs.hasNext()) {
                ((EntityBLocal) entityBs.next()).setLastModified(new Date());
            }
        } catch (Exception e) {
            sessionContext.setRollbackOnly();
            throw e;
        }
    }

    public void ejbCreate() throws CreateException {
    }

    public void ejbRemove() {
    }

    public void ejbActivate() {
    }

    public void ejbPassivate() {
    }

    public void setSessionContext(SessionContext context) {
        this.sessionContext = context;
    }

    private Context getContext() {
        if (context == null) {
            try {
                context = new InitialContext();
            } catch (NamingException e) {
                throw new EJBException(e.getMessage());
            }
        }
        return context;
    }

    private EntityALocalHome getEntityALocalHome() {
        if (entityALocalHome == null) {
            try {
                entityALocalHome = (EntityALocalHome) getContext().lookup
                        ("java:module/EntityA!org.jboss.as.test.integration.ejb.entity.cmp.optimisticlock.bug1006723.testentity.EntityALocalHome");
            } catch (NamingException e) {
                throw new EJBException(e.getMessage());
            }
        }
        return entityALocalHome;
    }

    private EntityBLocalHome getEntityBLocalHome() {
        if (entityBLocalHome == null) {
            try {
                entityBLocalHome = (EntityBLocalHome) getContext().lookup
                        ("java:module/EntityB!org.jboss.as.test.integration.ejb.entity.cmp.optimisticlock.bug1006723.testentity.EntityBLocalHome");
            } catch (NamingException e) {
                throw new EJBException(e.getMessage());
            }
        }
        return entityBLocalHome;
    }

    private SessionContext sessionContext;
    private Context context;
    private EntityALocalHome entityALocalHome;
    private EntityBLocalHome entityBLocalHome;

}

