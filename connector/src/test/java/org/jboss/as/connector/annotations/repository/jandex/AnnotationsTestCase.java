/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.connector.annotations.repository.jandex;

import org.jboss.jca.common.annotations.Annotations;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.jca.common.spi.annotations.repository.Annotation;
import org.jboss.jca.common.spi.annotations.repository.AnnotationRepository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VisitorAttributes;
import org.jboss.vfs.util.SuffixMatchFilter;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.io.Files;
import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Test cases for the annotations handling
 *
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 * @author <a href="mailto:jeff.zhang@jboss.org">Jeff Zhang</a>
 */
public class AnnotationsTestCase {
    /**
     * Annotations
     */
    private Annotations annotations;

    private static final String MULTIANNO_TARGET = "./target/test-classes/ra16inoutmultianno.rar";

    @Before
    public void setup() throws IOException {
        Files.createParentDirs(
                new File(MULTIANNO_TARGET + "/org/jboss/as/connector/deployers/spec/rars/ra16inoutmultianno/sample.file"));
        annotations = new Annotations();
    }

    @After
    public void tearDown() throws IOException {
        MoreFiles.deleteRecursively(Paths.get(MULTIANNO_TARGET, "org"), RecursiveDeleteOption.ALLOW_INSECURE);
        annotations = null;
    }

    /**
     * Process: Null arguemnt for annotation repository
     *
     * @throws Throwable throwable exception
     */
    @Test(expected = ValidateException.class)
    public void testProcessNullAnnotationRepository() throws Throwable {
        annotations.process(null, null, null);
    }

    /**
     * Process: Connector -- verification of the processConnector method
     *
     * @throws Throwable throwable exception
     */
    @Test
    public void testProcessConnector() throws Throwable {
        // Test prep
        File srcRars = new File("./target/test-classes/org/jboss/as/connector/deployers/spec/rars");
        File srcRa16 = new File("./target/test-classes/org/jboss/as/connector/deployers/spec/rars/ra16inoutmultianno");
        final String destRars = MULTIANNO_TARGET + "/org/jboss/as/connector/deployers/spec/rars";
        final String destRa16 = MULTIANNO_TARGET + "/org/jboss/as/connector/deployers/spec/rars/ra16inoutmultianno";
        for (File srcFile : srcRars.listFiles()) {
            if (!srcFile.isDirectory()) {
                Files.copy(srcFile, new File(destRars + "/" + srcFile.getName()));
            }
        }
        for (File srcFile : srcRa16.listFiles()) {
            if (!srcFile.isDirectory()) {
                Files.copy(srcFile, new File(destRa16 + "/" + srcFile.getName()));
            }
        }

        URI uri = getURI("/ra16inoutmultianno.rar");
        final VirtualFile virtualFile = VFS.getChild(uri);
        final Indexer indexer = new Indexer();
        final List<VirtualFile> classChildren = virtualFile
                .getChildren(new SuffixMatchFilter(".class", VisitorAttributes.RECURSE_LEAVES_ONLY));
        for (VirtualFile classFile : classChildren) {
            System.out.println("testProcessConnector: adding " + classFile.getPathName());
            InputStream inputStream = null;
            try {
                inputStream = classFile.openStream();
                indexer.index(inputStream);
            } finally {
                VFSUtils.safeClose(inputStream);
            }
        }
        final Index index = indexer.complete();
        AnnotationRepository ar = new JandexAnnotationRepositoryImpl(index, Thread.currentThread().getContextClassLoader());

        Collection<Annotation> values = ar.getAnnotation(javax.resource.spi.Connector.class);
        assertNotNull(values);
        assertEquals(1, values.size());

        // Test run
        try {
            annotations.process(ar, null, Thread.currentThread().getContextClassLoader());
        } catch (Throwable t) {
            t.printStackTrace();
            fail(t.getMessage());
        }
    }

    /**
     * Process: Connector -- verification of the processConnector method
     *
     * @throws Throwable throwable exception
     */
    @Test(expected = ValidateException.class)
    public void testProcessConnectorFailTooManyConnectors() throws Throwable {
        // Test prep
        AnnotationRepository ar = null;
        try {
            File srcRars = new File("./target/test-classes/org/jboss/as/connector/deployers/spec/rars");
            File srcRa16 = new File("./target/test-classes/org/jboss/as/connector/deployers/spec/rars/ra16inoutmultianno");
            File srcStandalone = new File("./target/test-classes/org/jboss/as/connector/deployers/spec/rars/rastandalone");
            final String destRars = MULTIANNO_TARGET + "/org/jboss/as/connector/deployers/spec/rars";
            final String destRa16 = MULTIANNO_TARGET + "/org/jboss/as/connector/deployers/spec/rars/ra16inoutmultianno";
            final String destStandalone = MULTIANNO_TARGET + "/org/jboss/as/connector/deployers/spec/rars/rastandalone";
            Files.createParentDirs(new File(destStandalone + "/sample.file"));
            for (File srcFile : srcRars.listFiles()) {
                if (!srcFile.isDirectory()) {
                    Files.copy(srcFile, new File(destRars + "/" + srcFile.getName()));
                }
            }
            for (File srcFile : srcRa16.listFiles()) {
                if (!srcFile.isDirectory()) {
                    Files.copy(srcFile, new File(destRa16 + "/" + srcFile.getName()));
                }
            }
            for (File srcFile : srcStandalone.listFiles()) {
                if (!srcFile.isDirectory()) {
                    Files.copy(srcFile, new File(destStandalone + "/" + srcFile.getName()));
                }
            }

            URI uri = getURI("/ra16inoutmultianno.rar");
            final VirtualFile virtualFile = VFS.getChild(uri);
            final Indexer indexer = new Indexer();
            final List<VirtualFile> classChildren = virtualFile
                    .getChildren(new SuffixMatchFilter(".class", VisitorAttributes.RECURSE_LEAVES_ONLY));
            for (VirtualFile classFile : classChildren) {
                System.out.println("testProcessConnectorFailTooManyConnectors: adding " + classFile.getPathName());
                InputStream inputStream = null;
                try {
                    inputStream = classFile.openStream();
                    indexer.index(inputStream);
                } finally {
                    VFSUtils.safeClose(inputStream);
                }
            }
            final Index index = indexer.complete();
            ar = new JandexAnnotationRepositoryImpl(index, Thread.currentThread().getContextClassLoader());
        } catch (Throwable e) {
            e.printStackTrace();
            fail("Test preparation error " + e.getMessage());
        }
        Collection<Annotation> values = ar.getAnnotation(javax.resource.spi.Connector.class);
        assertNotNull(values);
        assertEquals(2, values.size());

        // Test run
        annotations.process(ar, null, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Process: Connector -- verification of the processConnector method
     *
     * @throws Throwable throwable exception
     */
    @Ignore("JBJCA-1435: The current validation in IronJacamar is not correct, the testcase here is.")
    @Test(expected = ValidateException.class)
    public void testProcessConnectorFailNoConnector() throws Throwable {
        // Test prep
        AnnotationRepository ar = null;
        try {
            File srcRars = new File("./target/test-classes/org/jboss/as/connector/deployers/spec/rars");
            File srcRa16 = new File("./target/test-classes/org/jboss/as/connector/deployers/spec/rars/ra16inoutmultianno");
            final String destRars = MULTIANNO_TARGET + "/org/jboss/as/connector/deployers/spec/rars";
            final String destRa16 = MULTIANNO_TARGET + "/org/jboss/as/connector/deployers/spec/rars/ra16inoutmultianno";
            for (File srcFile : srcRars.listFiles()) {
                if (!srcFile.isDirectory() && !"BaseResourceAdapter.class".equals(srcFile.getName())) {
                    Files.copy(srcFile, new File(destRars + "/" + srcFile.getName()));
                }
            }
            for (File srcFile : srcRa16.listFiles()) {
                if (!srcFile.isDirectory() && !"TestResourceAdapter.class".equals(srcFile.getName())) {
                    Files.copy(srcFile, new File(destRa16 + "/" + srcFile.getName()));
                }
            }

            URI uri = getURI("/ra16inoutmultianno.rar");
            final VirtualFile virtualFile = VFS.getChild(uri);
            final Indexer indexer = new Indexer();
            final List<VirtualFile> classChildren = virtualFile
                    .getChildren(new SuffixMatchFilter(".class", VisitorAttributes.RECURSE_LEAVES_ONLY));
            for (VirtualFile classFile : classChildren) {
                System.out.println("testProcessConnectorFailNoConnector: adding " + classFile.getPathName());
                InputStream inputStream = null;
                try {
                    inputStream = classFile.openStream();
                    indexer.index(inputStream);
                } finally {
                    VFSUtils.safeClose(inputStream);
                }
            }
            final Index index = indexer.complete();
            ar = new JandexAnnotationRepositoryImpl(index, Thread.currentThread().getContextClassLoader());
        } catch (Throwable e) {
            e.printStackTrace();
            fail("Test preparation error " + e.getMessage());
        }
        Collection<Annotation> values = ar.getAnnotation(javax.resource.spi.Connector.class);
        assertNull(values);

        // Test run
        annotations.process(ar, null, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Process: Connector -- verification of the processConnector method
     *
     * @throws Throwable throwable exception
     */
    public void testProcessConnectorManualSpecConnector() throws Throwable {
        // Test prep
        AnnotationRepository ar = null;
        try {
            File srcRars = new File("./target/test-classes/org/jboss/as/connector/deployers/spec/rars");
            File srcRa16 = new File("./target/test-classes/org/jboss/as/connector/deployers/spec/rars/ra16inoutmultianno");
            final String destRars = MULTIANNO_TARGET + "/org/jboss/as/connector/deployers/spec/rars";
            final String destRa16 = MULTIANNO_TARGET + "/org/jboss/as/connector/deployers/spec/rars/ra16inoutmultianno";
            for (File srcFile : srcRars.listFiles()) {
                if (!srcFile.isDirectory()) {
                    Files.copy(srcFile, new File(destRars + "/" + srcFile.getName()));
                }
            }
            for (File srcFile : srcRa16.listFiles()) {
                if (!srcFile.isDirectory() && !"TestResourceAdapter.class".equals(srcFile.getName())) {
                    Files.copy(srcFile, new File(destRa16 + "/" + srcFile.getName()));
                }
            }

            URI uri = getURI("/ra16inoutmultianno.rar");
            final VirtualFile virtualFile = VFS.getChild(uri);
            final Indexer indexer = new Indexer();
            final List<VirtualFile> classChildren = virtualFile
                    .getChildren(new SuffixMatchFilter(".class", VisitorAttributes.RECURSE_LEAVES_ONLY));
            for (VirtualFile classFile : classChildren) {
                System.out.println("testProcessConnectorFailNoConnector: adding " + classFile.getPathName());
                InputStream inputStream = null;
                try {
                    inputStream = classFile.openStream();
                    indexer.index(inputStream);
                } finally {
                    VFSUtils.safeClose(inputStream);
                }
            }
            final Index index = indexer.complete();
            ar = new JandexAnnotationRepositoryImpl(index, Thread.currentThread().getContextClassLoader());
        } catch (Throwable e) {
            e.printStackTrace();
            fail("Test preparation error " + e.getMessage());
        }
        Collection<Annotation> values = ar.getAnnotation(javax.resource.spi.Connector.class);
        assertNull(values);

        // Test run
        annotations.process(ar, "org.jboss.as.connector.deployers.spec.rars.BaseResourceAdapter", Thread.currentThread().getContextClassLoader());
    }

    /**
     * Get the URL for a test archive
     *
     * @param archive The name of the test archive
     * @return The URL to the archive
     * @throws Throwable throwable exception
     */
    public URI getURI(String archive) throws Throwable {
        return this.getClass().getResource(archive).toURI();
    }
}
