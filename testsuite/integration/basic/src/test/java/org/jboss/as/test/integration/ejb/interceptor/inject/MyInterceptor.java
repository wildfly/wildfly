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
