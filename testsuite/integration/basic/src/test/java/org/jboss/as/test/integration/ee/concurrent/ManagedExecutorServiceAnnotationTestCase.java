/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.concurrent;

import jakarta.annotation.Resource;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test for ManagedExecutorService defined via @ManagedExecutorDefinition
 *
 * @author Ivo Studensky
 */
@ExtendWith(ArquillianExtension.class)
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
        Assertions.assertNotNull(service);
        service.testRunnable(ManagedExecutorServiceAnnotationTestCase::testMethod).get();
    }
}
