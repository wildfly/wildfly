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

package org.jboss.as.controller.client.helpers.standalone.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.Assert;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlan;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentPlanResult;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for behavior re: cleaning up InputStreams, e.g. AS7-2392.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class InputStreamCleanupTestCase {

    private static File baseDir;
    private static long count = System.currentTimeMillis();

    @BeforeClass
    public static void beforeClass() {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        baseDir = new File(tmpDir, InputStreamCleanupTestCase.class.getSimpleName());
        baseDir.deleteOnExit();
        baseDir.mkdirs();
    }

    @AfterClass
    public static void afterClass() {
        File[] children = baseDir.listFiles();
        if (children != null) {
            for (File child : children) {
                child.delete();
            }
        }
        baseDir.delete();
    }

    private InputStream testStream;

    @After
    public void after() throws IOException {
        if (testStream != null) {
            testStream.close();
        }
    }

    @Test
    public void testFileSuccess() throws IOException {
        ServerDeploymentManager sdm = new MockServerDeploymentManager();
        DeploymentPlanBuilder builder = sdm.newDeploymentPlan();
        builder = builder.add("test", createTempFile());
        DeploymentPlanImpl planImpl = getDeploymentPlanImpl(builder);
        safeGet(sdm, planImpl);
        InputStream is = getInputStream(planImpl);
        assertClosed(is);
    }

    @Test
    public void testURLSuccess() throws IOException {
        ServerDeploymentManager sdm = new MockServerDeploymentManager();
        DeploymentPlanBuilder builder = sdm.newDeploymentPlan();
        builder = builder.add("test", createTempURL());
        DeploymentPlanImpl planImpl = getDeploymentPlanImpl(builder);
        safeGet(sdm, planImpl);
        InputStream is = getInputStream(planImpl);
        assertClosed(is);
    }

    @Test
    public void testStreamSuccess() throws IOException {
        ServerDeploymentManager sdm = new MockServerDeploymentManager();
        DeploymentPlanBuilder builder = sdm.newDeploymentPlan();
        createTempInputStream();
        builder = builder.add("test", testStream);
        DeploymentPlanImpl planImpl = getDeploymentPlanImpl(builder);
        safeGet(sdm, planImpl);
        assertNotClosed(testStream);
    }

    @Test
    public void testSuccessfulCancel() throws IOException {
        ServerDeploymentManager sdm = new MockServerDeploymentManager(true);
        DeploymentPlanBuilder builder = sdm.newDeploymentPlan();
        builder = builder.add("test", createTempFile());
        DeploymentPlanImpl planImpl = getDeploymentPlanImpl(builder);
        safeCancel(sdm, planImpl);
        InputStream is = getInputStream(planImpl);
        assertClosed(is);
    }

    @Test
    public void testUnsuccessfulCancel() throws IOException {
        ServerDeploymentManager sdm = new MockServerDeploymentManager(false);
        DeploymentPlanBuilder builder = sdm.newDeploymentPlan();
        builder = builder.add("test", createTempFile());
        DeploymentPlanImpl planImpl = getDeploymentPlanImpl(builder);
        safeCancel(sdm, planImpl);
        InputStream is = getInputStream(planImpl);
        assertNotClosed(is);
    }

    @Test
    public void testInterrupted() throws IOException {
        ServerDeploymentManager sdm = new MockServerDeploymentManager(new InterruptedException());
        DeploymentPlanBuilder builder = sdm.newDeploymentPlan();
        builder = builder.add("test", createTempFile());
        DeploymentPlanImpl planImpl = getDeploymentPlanImpl(builder);
        safeGet(sdm, planImpl);
        InputStream is = getInputStream(planImpl);
        assertNotClosed(is);

        builder = sdm.newDeploymentPlan();
        builder = builder.add("test", createTempFile());
        planImpl = getDeploymentPlanImpl(builder);
        safeGetWithTimeout(sdm, planImpl);
        is = getInputStream(planImpl);
        assertNotClosed(is);
    }

    @Test
    public void testTimeoutException() throws IOException {
        ServerDeploymentManager sdm = new MockServerDeploymentManager(new TimeoutException());
        DeploymentPlanBuilder builder = sdm.newDeploymentPlan();
        builder = builder.add("test", createTempFile());
        DeploymentPlanImpl planImpl = getDeploymentPlanImpl(builder);
        safeGetWithTimeout(sdm, planImpl);
        InputStream is = getInputStream(planImpl);
        assertNotClosed(is);
    }

    @Test
    public void testExecutionException() throws IOException {
        ServerDeploymentManager sdm = new MockServerDeploymentManager(new ExecutionException(new Exception()));
        DeploymentPlanBuilder builder = sdm.newDeploymentPlan();
        builder = builder.add("test", createTempFile());
        DeploymentPlanImpl planImpl = getDeploymentPlanImpl(builder);
        safeGet(sdm, planImpl);
        InputStream is = getInputStream(planImpl);
        assertClosed(is);

        builder = sdm.newDeploymentPlan();
        builder = builder.add("test", createTempFile());
        planImpl = getDeploymentPlanImpl(builder);
        safeGetWithTimeout(sdm, planImpl);
        is = getInputStream(planImpl);
        assertClosed(is);
    }

    @Test
    public void testRuntimeException() throws IOException {
        ServerDeploymentManager sdm = new MockServerDeploymentManager(new RuntimeException());
        DeploymentPlanBuilder builder = sdm.newDeploymentPlan();
        builder = builder.add("test", createTempFile());
        DeploymentPlanImpl planImpl = getDeploymentPlanImpl(builder);
        safeGet(sdm, planImpl);
        InputStream is = getInputStream(planImpl);
        assertClosed(is);

        builder = sdm.newDeploymentPlan();
        builder = builder.add("test", createTempFile());
        planImpl = getDeploymentPlanImpl(builder);
        safeGetWithTimeout(sdm, planImpl);
        is = getInputStream(planImpl);
        assertClosed(is);
    }

    private static File createTempFile() throws IOException {
        File file = new File(baseDir, String.valueOf(count++));
        file.deleteOnExit();
        file.createNewFile();
        return file;
    }

    private static URL createTempURL() throws IOException {
        return createTempFile().toURI().toURL();
    }

    private InputStream createTempInputStream() throws IOException {
        this.testStream = new FileInputStream(createTempFile());
        return testStream;
    }

    private static DeploymentPlanImpl getDeploymentPlanImpl(DeploymentPlanBuilder builder) {
        return DeploymentPlanImpl.class.cast(builder.build());
    }

    private static void safeGet(ServerDeploymentManager sdm, DeploymentPlan plan) {
        Future<ServerDeploymentPlanResult> future = sdm.execute(plan);
        try {
            future.get();
        } catch (Exception ignored) {
            // ignore
        }
    }

    private static void safeGetWithTimeout(ServerDeploymentManager sdm, DeploymentPlan plan) {
        Future<ServerDeploymentPlanResult> future = sdm.execute(plan);
        try {
            future.get(1L, TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
            // ignore
        }
    }

    private static void safeCancel(ServerDeploymentManager sdm, DeploymentPlan plan) {
        Future<ServerDeploymentPlanResult> future = sdm.execute(plan);
        try {
            future.cancel(true);
        } catch (Exception ignored) {
            // ignore
        }
    }

    private static InputStream getInputStream(DeploymentPlanImpl planImpl) {
        for (DeploymentActionImpl action : planImpl.getDeploymentActionImpls()) {
            if (action.getContentStream() != null) {
                return action.getContentStream();
            }
        }
        throw new IllegalStateException("no content stream available");
    }

    private void assertClosed(InputStream is) {
        try {
            is.available();
            Assert.fail("InputStream is not closed");
        } catch (IOException good) {
            // we assume the IOE is because it's closed
        }
    }

    private void assertNotClosed(InputStream is) {
        try {
            is.available();
        } catch (IOException good) {
            // we assume the IOE is because it's closed
            Assert.fail("InputStream is closed");
        }
    }

    private static class MockServerDeploymentManager extends AbstractServerDeploymentManager {

        private Exception exception;
        private Boolean cancelled;

        private MockServerDeploymentManager() {
        }

        private MockServerDeploymentManager(boolean cancelled) {
            this.cancelled = cancelled;
        }

        private MockServerDeploymentManager(Exception e) {
            exception = e;
        }

        @Override
        protected Future<ModelNode> executeOperation(Operation context) {
            if (exception != null) {
                return new TestFuture(exception);
            } else if (cancelled != null) {
                return new TestFuture(cancelled);
            } else {
                return new TestFuture();
            }
        }
    }

    private static class TestFuture implements Future<ModelNode> {

        private Exception exception;
        private boolean cancelled;

        private TestFuture() {
        }

        private TestFuture(boolean cancelled) {
            this.cancelled = cancelled;
        }

        private TestFuture(Exception e) {
            exception = e;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return cancelled;
        }

        @Override
        public boolean isCancelled() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isDone() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ModelNode get() throws InterruptedException, ExecutionException {
            if (exception == null) {
                return new ModelNode();
            } else if (exception instanceof InterruptedException) {
                throw  (InterruptedException) exception;
            } else if (exception instanceof ExecutionException) {
                throw  (ExecutionException) exception;
            }
            throw (RuntimeException) exception;
        }

        @Override
        public ModelNode get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            if (exception == null) {
                return new ModelNode();
            } else if (exception instanceof InterruptedException) {
                throw  (InterruptedException) exception;
            } else if (exception instanceof ExecutionException) {
                throw  (ExecutionException) exception;
            } else if (exception instanceof TimeoutException) {
                throw  (TimeoutException) exception;
            }
            throw (RuntimeException) exception;
        }
    }
}
