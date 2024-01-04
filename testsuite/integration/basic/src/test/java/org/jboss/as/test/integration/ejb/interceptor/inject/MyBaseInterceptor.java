/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.inject;

import java.util.ArrayList;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import javax.sql.DataSource;
import jakarta.transaction.TransactionManager;

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
