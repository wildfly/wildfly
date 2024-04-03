/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.implicitcomponents;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.inject.Inject;

/**
 * Tests that managed beans an eligible for CDI injection, even with no beans.xml
 */
@RunWith(Arquillian.class)
public class ImplicitComponentTestCase {


    @Deployment
    public static JavaArchive deployment() {
        return ShrinkWrap.create(JavaArchive.class, "war-example.jar")
                .addPackage(ImplicitComponentTestCase.class.getPackage());
    }

    @Inject
    private ManagedBean1 bean1;

    @Test
    public void testManagedBeanInjection() {
        Assert.assertEquals(ManagedBean2.MESSAGE, bean1.getMessage());
    }
}
