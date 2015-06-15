/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.jca.moduledeployment;

import java.io.File;

import org.jboss.as.test.deployment.AbstractModuleDeployment;
import org.jboss.as.test.integration.jca.rar.MultipleConnectionFactory1;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;

/**
 * AS7-5768 -Support for RA module deployment
 *
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 * @author <a href="istudens@redhat.com">Ivo Studensky</a>
 */
public abstract class AbstractModuleDeploymentTestCaseSetup extends AbstractModuleDeployment {
    private static final Logger log = Logger.getLogger(AbstractModuleDeploymentTestCaseSetup.class);

    /**
     * Creates module structure for uncompressed RA archive. RA classes are in
     * flat form too
     *
     * @throws Exception
     */
    protected void fillModuleWithFlatClasses(String raFile) throws Exception {
        ResourceAdapterArchive rar = ShrinkWrap
                .create(ResourceAdapterArchive.class);
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ra16out.jar");
        jar.addPackage(MultipleConnectionFactory1.class.getPackage()).addClass(
                javax.jms.MessageListener.class);
        rar.addAsManifestResource(this.getClass().getPackage(), raFile,
                "ra.xml");
        rar.as(ExplodedExporter.class).exportExploded(testModuleRoot, getSlot());
        jar.as(ExplodedExporter.class).exportExploded(testModuleRoot, getSlot());
    }

    /**
     * Creates module structure for uncompressed RA archive.
     * RA classes are packed in .jar archive
     *
     * @throws Exception
     */
    protected void fillModuleWithJar(String raFile) throws Exception {
        ResourceAdapterArchive rar = ShrinkWrap
                .create(ResourceAdapterArchive.class);
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ra16out.jar");
        jar.addPackage(MultipleConnectionFactory1.class.getPackage());
        rar.addAsManifestResource(
                PureJarTestCase.class.getPackage(), raFile, "ra.xml");
        rar.as(ExplodedExporter.class).exportExploded(testModuleRoot, getSlot());

        copyFile(new File(slot, "ra16out.jar"), jar.as(ZipExporter.class).exportAsInputStream());
    }
}
