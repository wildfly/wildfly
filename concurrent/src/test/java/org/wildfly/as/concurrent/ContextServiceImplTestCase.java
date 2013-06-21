/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.as.concurrent;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.wildfly.as.concurrent.context.ContextConfiguration;
import org.wildfly.as.concurrent.context.TestContext;
import org.wildfly.as.concurrent.context.TestContextConfiguration;
import org.wildfly.as.concurrent.context.TestRunnable;

/**
 *
 */
public class ContextServiceImplTestCase {

    protected ContextServiceImpl contextService;

    protected ContextServiceImpl newContextService(ContextConfiguration contextConfiguration) {
        return new ContextServiceImpl(contextConfiguration);
    }

    @Before
    public void beforeTest() {
        final TestContextConfiguration contextConfiguration = new TestContextConfiguration();
        contextService = newContextService(contextConfiguration);
        Assert.assertTrue(TestContext.allContexts.isEmpty());
    }

    @Test
    public void testNonObjectInvocation() throws Exception {
        final TestRunnable testRunnable = new TestRunnable();
        Runnable runnable = contextService.createContextualProperty(testRunnable, Runnable.class);
        testRunnable.assertContextIsNotSet();
        runnable.run();
        testRunnable.assertContextWasSet();
        testRunnable.assertContextWasReset();
    }

    @Test
    public void testObjectInvocation() throws Exception {
        final TestRunnable testRunnable = new TestRunnable();
        Runnable runnable = contextService.createContextualProperty(testRunnable, Runnable.class);
        testRunnable.assertContextIsNotSet();
        runnable.hashCode();
        testRunnable.assertContextIsNotSet();
    }

    @After
    public void afterTest() {
        Assert.assertTrue(TestContext.allContexts.isEmpty());
    }

}
