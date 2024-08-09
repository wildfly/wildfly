/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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
