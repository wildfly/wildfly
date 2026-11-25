/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.concurrent;

import jakarta.annotation.Resource;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for ManagedExecutorService defined via @ManagedExecutorDefinition
 *
 * @author Ivo Studensky
 */
@RunWith(Arquillian.class)
public class ManagedExecutorServiceAnnotationTestCase {

    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(JavaArchive.class, ManagedExecutorServiceAnnotationTestCase.class.getSimpleName() + ".jar")
                .addClasses(ManagedExecutorServiceAnnotationTestCase.class, ManagedExecutorServiceAnnotationBean.class);
    }

    @Resource(lookup = "java:module/ManagedExecutorServiceAnnotationBean")
    ManagedExecutorServiceAnnotationBean service;

    private static void testMethod() {
        // empty method
    }

    @Test
    public void testExecutorService() throws Exception {
        Assert.assertNotNull(service);
        service.testRunnable(ManagedExecutorServiceAnnotationTestCase::testMethod).get();
    }
}
