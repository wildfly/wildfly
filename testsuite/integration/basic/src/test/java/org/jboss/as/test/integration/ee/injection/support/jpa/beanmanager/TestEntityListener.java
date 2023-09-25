/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.support.jpa.beanmanager;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

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
