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
package org.jboss.as.test.integration.ee.injection.support.jpa.beanManager;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

public class TestEntityListener {

    @PrePersist
    public void prePersist(Object obj) throws NamingException {
        Foo foo = obtainFooViaCdiCurrent();
        Bar bar = obtainBarViaBMFromJNDI();
        foo.ping();
        bar.ping();
    }

    @PreUpdate
    public void preUpdate(Object obj) throws NamingException {
        Foo foo = obtainFooViaCdiCurrent();
        Bar bar = obtainBarViaBMFromJNDI();
        foo.ping();
        bar.ping();
    }

    @PreRemove
    public void preRemove(Object obj) throws NamingException {
        Foo foo = obtainFooViaCdiCurrent();
        Bar bar = obtainBarViaBMFromJNDI();
        foo.ping();
        bar.ping();
    }

    @PostLoad
    public void postLoad(Object obj) throws NamingException {
        Foo foo = obtainFooViaCdiCurrent();
        Bar bar = obtainBarViaBMFromJNDI();
        foo.ping();
        bar.ping();
    }

    @PostUpdate
    public void postUpdate(Object obj) throws NamingException {
        Foo foo = obtainFooViaCdiCurrent();
        Bar bar = obtainBarViaBMFromJNDI();
        foo.ping();
        bar.ping();
    }

    @PostPersist
    public void postPersist(Object obj) throws NamingException {
        Foo foo = obtainFooViaCdiCurrent();
        Bar bar = obtainBarViaBMFromJNDI();
        foo.ping();
        bar.ping();
    }

    @PostRemove
    public void postRemove(Object obj) throws NamingException {
        Foo foo = obtainFooViaCdiCurrent();
        Bar bar = obtainBarViaBMFromJNDI();
        foo.ping();
        bar.ping();
    }

    private Foo obtainFooViaCdiCurrent() {
        BeanManager cdiCurrentBeanManager = CDI.current().getBeanManager();
        Bean<Foo> bean = (Bean<Foo>) cdiCurrentBeanManager.getBeans(Foo.class).stream().findFirst().get();
        CreationalContext<Foo> creationalContext = cdiCurrentBeanManager.createCreationalContext(bean);
        Foo fooBean = (Foo) cdiCurrentBeanManager.getReference(bean, Foo.class, creationalContext);
        return fooBean;
    }

    private Bar obtainBarViaBMFromJNDI() throws NamingException {
        BeanManager bm = (BeanManager) new InitialContext().lookup("java:comp/BeanManager");
        Bean<Bar> bean = (Bean<Bar>) bm.getBeans(Bar.class).stream().findFirst().get();
        CreationalContext<Bar> creationalContext = bm.createCreationalContext(bean);
        Bar bar = (Bar) bm.getReference(bean, Bar.class, creationalContext);
        return bar;
    }

}
