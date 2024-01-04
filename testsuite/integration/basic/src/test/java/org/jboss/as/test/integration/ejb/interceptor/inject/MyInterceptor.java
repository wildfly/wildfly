/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.inject;

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
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 */
public class MyInterceptor extends MyBaseInterceptor {
    @EJB
    MySession2 session2;
    @Resource(mappedName = "java:/TransactionManager")
    TransactionManager tm;
    @Resource(name = "DefaultDS", mappedName = "java:jboss/datasources/ExampleDS")
    DataSource ds;
    @PersistenceContext(unitName = "interceptors-test")
    EntityManager em;
    @PersistenceContext(unitName = "interceptors-test")
    Session session;
    @PersistenceUnit(unitName = "interceptors-test")
    EntityManagerFactory factory;
    @PersistenceUnit(unitName = "interceptors-test")
    SessionFactory sessionFactory;

    MySession2 session2Method;
    TransactionManager tmMethod;
    DataSource dsMethod;
    EntityManager emMethod;
    Session sessionMethod;
    EntityManagerFactory factoryMethod;
    SessionFactory sessionFactoryMethod;

    @EJB
    public void setSession2Method(MySession2 session2Method) {
        this.session2Method = session2Method;
    }

    @Resource(mappedName = "java:/TransactionManager")
    public void setTmMethod(TransactionManager tmMethod) {
        this.tmMethod = tmMethod;
    }

    @Resource(name = "DefaultDS", mappedName = "java:DefaultDS")
    public void setDsMethod(DataSource dsMethod) {
        this.dsMethod = dsMethod;
    }

    @PersistenceContext(unitName = "interceptors-test")
    public void setEmMethod(EntityManager emMethod) {
        this.emMethod = emMethod;
    }

    @PersistenceContext(unitName = "interceptors-test")
    public void setSessionMethod(Session sessionMethod) {
        this.sessionMethod = sessionMethod;
    }

    @PersistenceUnit(unitName = "interceptors-test")
    public void setFactoryMethod(EntityManagerFactory factoryMethod) {
        this.factoryMethod = factoryMethod;
    }

    @PersistenceUnit(unitName = "interceptors-test")
    public void setSessionFactoryMethod(SessionFactory sessionFactoryMethod) {
        this.sessionFactoryMethod = sessionFactoryMethod;
    }

    @AroundInvoke
    public Object invoke(InvocationContext ctx) throws Exception {
        session2.doit();
        if (tm == null) { throw new RuntimeException("tm was null"); }
        if (ds == null) { throw new RuntimeException("ds was null"); }
        if (em == null) { throw new RuntimeException("em was null"); }
        if (session == null) { throw new RuntimeException("session was null"); }
        if (factory == null) { throw new RuntimeException("factory was null"); }
        if (sessionFactory == null) { throw new RuntimeException("sessionFactory was null"); }

        session2Method.doit();
        if (tmMethod == null) { throw new RuntimeException("tm was null"); }
        if (dsMethod == null) { throw new RuntimeException("ds was null"); }
        if (emMethod == null) { throw new RuntimeException("em was null"); }
        if (sessionMethod == null) { throw new RuntimeException("session was null"); }
        if (factoryMethod == null) { throw new RuntimeException("factory was null"); }
        if (sessionFactoryMethod == null) { throw new RuntimeException("sessionFactory was null"); }
        return ctx.proceed();
    }
}
