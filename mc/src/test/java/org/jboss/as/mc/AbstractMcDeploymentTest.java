/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.mc;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.model.ServerGroupDeploymentElement;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.junit.After;
import org.junit.Before;

/**
 * @author John E. Bailey
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class AbstractMcDeploymentTest {
    protected static final byte[] BLANK_SHA1 = new byte[20];

    protected ServiceContainer serviceContainer;

    @Before
    public void setup() throws Exception {

        serviceContainer = ServiceContainer.Factory.create();

        runWithLatchedBatch(new BatchedWork() {
            @Override
            public void execute(BatchBuilder batchBuilder, ServiceContainer serviceContainer) throws Exception {
                setupServices(batchBuilder);
            }
        });
    }

    protected void setupServices(final ServiceTarget serviceTarget) throws Exception {
        MockServerDeploymentRepository.addService(serviceTarget, VFS.getChild(getResource(AbstractMcDeploymentTest.class, "/test")).getPhysicalFile());
    }

    @After
    public void shutdown() {
        serviceContainer.shutdown();
    }

    protected void executeDeployment(final VirtualFile deploymentRoot) throws Exception {
        runWithLatchedBatch(new BatchedWork() {
            public void execute(BatchBuilder batchBuilder, ServiceContainer serviceContainer) throws Exception {
                ServerDeploymentTestSupport.deploy(new ServerGroupDeploymentElement(deploymentRoot.getName(), deploymentRoot.getName(), BLANK_SHA1, true), batchBuilder, serviceContainer);
            }
        });
    }

    protected String getDeploymentName(VirtualFile deploymentRoot) {
        return deploymentRoot.getName().replace('.', '_');
    }

    protected void runWithLatchedBatch(final BatchedWork work) throws Exception {
        final BatchBuilder batchBuilder = serviceContainer.batchBuilder();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean completed = new AtomicBoolean(false);
        final TestBeanListener listener = new TestBeanListener(new Runnable() {
            public void run() {
                completed.set(true);
                latch.countDown();
            }
        });
        batchBuilder.addListener(listener);
        // Run the work
        work.execute(batchBuilder, serviceContainer);

        batchBuilder.install();
        listener.finishBatch();
        latch.await(5L, TimeUnit.SECONDS);
        if (!completed.get())
            fail("Did not install deployment within 5 seconds.");
    }

    protected static interface BatchedWork {
        void execute(final BatchBuilder batchBuilder, final ServiceContainer serviceContainer) throws Exception;
    }

    protected URL getResource(final Class<?> testClass, final String path) throws Exception {
        return testClass.getResource(path);
    }

    protected File getResourceFile(final Class<?> testClass, final String path) throws Exception {
        return new File(getResource(testClass, path).toURI());
    }

    protected void copyResource(final Class<?> testClass, final String inputResource, final String outputBase, final String outputPath) throws Exception {
        final File resource = getResourceFile(testClass, inputResource);
        final File outputDirectory = new File(getResourceFile(testClass, outputBase), outputPath);

        if (!resource.exists())
            throw new IllegalArgumentException("Resource does not exist");
        if (outputDirectory.exists() && outputDirectory.isFile())
            throw new IllegalArgumentException("OutputDirectory must be a directory");
        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdirs())
                throw new RuntimeException("Failed to create output directory");
        }
        final File outputFile = new File(outputDirectory, resource.getName());
        final InputStream in = new FileInputStream(resource);
        try {
            final OutputStream out = new FileOutputStream(outputFile);
            try {
                final byte[] b = new byte[8192];
                int c;
                while ((c = in.read(b)) != -1) {
                    out.write(b, 0, c);
                }
                out.close();
                in.close();
            } finally {
                VFSUtils.safeClose(out);
            }
        } finally {
            VFSUtils.safeClose(in);
        }
    }
}
