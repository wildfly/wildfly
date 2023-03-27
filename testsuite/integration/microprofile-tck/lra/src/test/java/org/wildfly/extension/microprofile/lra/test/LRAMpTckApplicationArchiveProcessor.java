/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.microprofile.lra.test;

import io.narayana.lra.LRAConstants;
import io.narayana.lra.arquillian.spi.NarayanaLRARecovery;
import org.eclipse.microprofile.lra.tck.service.spi.LRARecoveryService;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import java.io.File;

/**
 * @author Martin Stefanko
 */
public class LRAMpTckApplicationArchiveProcessor implements ApplicationArchiveProcessor {

    @Override
    public void process(Archive<?> archive, TestClass testClass) {
        if (archive instanceof WebArchive) {
            JavaArchive extensionsJar = ShrinkWrap.create(JavaArchive.class, "extension.jar");

            extensionsJar.addClasses(NarayanaLRARecovery.class, LRAConstants.class);
            extensionsJar.addAsServiceProvider(LRARecoveryService.class, NarayanaLRARecovery.class);

            extensionsJar.addAsManifestResource(new StringAsset("<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"\n" +
                "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "       xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_4_0.xsd\"\n" +
                "       version=\"4.0\" bean-discovery-mode=\"all\">\n" +
                "</beans>"), "beans.xml");

            WebArchive war = (WebArchive) archive;
            war.addAsLibraries(extensionsJar);

            final File archiveDir = new File("target/archives");
            archiveDir.mkdirs();
            File moduleFile = new File(archiveDir, "test-lra-extension.war");
            war.as(ZipExporter.class).exportTo(moduleFile, true);
        }
    }
}
