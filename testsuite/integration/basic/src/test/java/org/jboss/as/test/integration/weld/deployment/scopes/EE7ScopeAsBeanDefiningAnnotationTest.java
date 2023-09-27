/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.deployment.scopes;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class EE7ScopeAsBeanDefiningAnnotationTest {

    @Inject
    private BeanManager manager;

    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(WebArchive.class).addClasses(ViewScopedBean.class, FlowScopedBean.class, TransactionScopedBean.class);
    }

    @Test
    public void testViewScopedBeanDiscovered() {
        Assert.assertNotNull(manager.resolve(manager.getBeans(ViewScopedBean.class)));
    }

    @Test
    public void testFlowScopedBeanDiscovered() {
        Assert.assertNotNull(manager.resolve(manager.getBeans(FlowScopedBean.class)));
    }

    @Test
    public void testTransactionScopedBeanDiscovered() {
        Assert.assertNotNull(manager.resolve(manager.getBeans(TransactionScopedBean.class)));
    }
}
