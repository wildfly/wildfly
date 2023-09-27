/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.annotation;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.ejb.EJB;

/**
 * Test that a bean which uses {@link jakarta.ejb.TransactionManagement} annotation
 * without any explicit value, doesn't cause a deployment failure.
 *
 * @see https://issues.jboss.org/browse/AS7-1506
 *      <p/>
 *      User: Jaikiran Pai
 */
@RunWith(Arquillian.class)
public class NoExplicitTransactionManagementValueTestCase {

    @EJB(mappedName = "java:module/BeanWithoutTransactionManagementValue")
    private BeanWithoutTransactionManagementValue bean;

    @Deployment
    public static Archive<?> createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "as7-1506.jar");
        jar.addClass(BeanWithoutTransactionManagementValue.class);

        return jar;
    }

    /**
     * Test that the deployment of the bean was successful and invocation on the bean
     * works
     */
    @Test
    public void testSuccessfulDeployment() {
        this.bean.doNothing();
    }
}
