/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.interceptor.inject;

import java.util.ArrayList;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MyBaseInterceptor {
    @EJB
    MySession2 baseSession2;
    @Resource(mappedName = "java:/TransactionManager")
    TransactionManager baseTm;
    @Resource(name = "DefaultDS", mappedName = "java:jboss/datasources/ExampleDS")
    DataSource baseDs;
    @PersistenceContext(unitName = "interceptors-test")
    EntityManager baseEm;
    @PersistenceContext(unitName = "interceptors-test")
    Session baseSession;
    @PersistenceUnit(unitName = "interceptors-test")
    EntityManagerFactory baseFactory;
    @PersistenceUnit(unitName = "interceptors-test")
    SessionFactory baseSessionFactory;

    MySession2 baseSession2Method;
    TransactionManager baseTmMethod;
    DataSource baseDsMethod;
    EntityManager baseEmMethod;
    Session baseSessionMethod;
    EntityManagerFactory baseFactoryMethod;
    SessionFactory baseSessionFactoryMethod;

    @EJB
    public void setBaseSession2Method(MySession2 session2Method) {
        this.baseSession2Method = session2Method;
    }

    @Resource(mappedName = "java:/TransactionManager")
    public void setBaseTmMethod(TransactionManager tmMethod) {
        this.baseTmMethod = tmMethod;
    }

    @Resource(name = "DefaultDS", mappedName = "java:DefaultDS")
    public void setBaseDsMethod(DataSource dsMethod) {
        this.baseDsMethod = dsMethod;
    }

    @PersistenceContext(unitName = "interceptors-test")
    public void setBaseEmMethod(EntityManager emMethod) {
        this.baseEmMethod = emMethod;
    }

    @PersistenceContext(unitName = "interceptors-test")
    public void setBaseSessionMethod(Session sessionMethod) {
        this.baseSessionMethod = sessionMethod;
    }

    @PersistenceUnit(unitName = "interceptors-test")
    public void setBaseFactoryMethod(EntityManagerFactory factoryMethod) {
        this.baseFactoryMethod = factoryMethod;
    }

    @PersistenceUnit(unitName = "interceptors-test")
    public void setBaseSessionFactoryMethod(SessionFactory sessionFactoryMethod) {
        this.baseSessionFactoryMethod = sessionFactoryMethod;
    }

    @AroundInvoke
    public Object baseInvoke(InvocationContext ctx) throws Exception {
        baseSession2.doit();
        if (baseTm == null) { throw new RuntimeException("tm was null"); }
        if (baseDs == null) { throw new RuntimeException("ds was null"); }
        if (baseEm == null) { throw new RuntimeException("em was null"); }
        if (baseSession == null) { throw new RuntimeException("session was null"); }
        if (baseFactory == null) { throw new RuntimeException("factory was null"); }
        if (baseSessionFactory == null) { throw new RuntimeException("sessionFactory was null"); }

        baseSession2Method.doit();
        if (baseTmMethod == null) { throw new RuntimeException("tm was null"); }
        if (baseDsMethod == null) { throw new RuntimeException("ds was null"); }
        if (baseEmMethod == null) { throw new RuntimeException("em was null"); }
        if (baseSessionMethod == null) { throw new RuntimeException("session was null"); }
        if (baseFactoryMethod == null) { throw new RuntimeException("factory was null"); }
        if (baseSessionFactoryMethod == null) { throw new RuntimeException("sessionFactory was null"); }
        ArrayList list = (ArrayList) ctx.proceed();
        list.add(0, "MyBaseInterceptor");
        return list;
    }

}
