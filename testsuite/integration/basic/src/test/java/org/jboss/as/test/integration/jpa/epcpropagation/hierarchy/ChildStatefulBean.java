/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.epcpropagation.hierarchy;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateful;
import javax.naming.InitialContext;

import org.junit.Assert;

/**
 * @author Stuart Douglas
 */
@Stateful
public class ChildStatefulBean extends BeanParent {

    @EJB
    private SimpleStatefulBean injectedSimpleStatefulBean;

    public void testPropagation() throws Exception {
        SimpleStatefulBean simpleStatefulBean = (SimpleStatefulBean) new InitialContext().lookup("java:module/" + SimpleStatefulBean.class.getSimpleName());
        Bus b = new Bus(1, "My Bus");
        entityManager.persist(b);
        //the XPC should propagate
        simpleStatefulBean.noop();
        injectedSimpleStatefulBean.noop();
        Assert.assertTrue(simpleStatefulBean.contains(b));
        Assert.assertTrue(injectedSimpleStatefulBean.contains(b));
        Assert.assertTrue(entityManager.contains(b));
    }
}
