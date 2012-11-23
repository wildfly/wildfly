/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.domain.extension;

import org.jboss.as.controller.Extension;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.StreamExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

/**
 * @author Emanuel Muckenhuber
 */
public class ExtensionSetup {

    public static void initializeTestExtension(final DomainTestSupport support) throws IOException {
        // Get module.xml, create modules.jar and add to test config
        final InputStream moduleXml = getModuleXml("module.xml");
        StreamExporter exporter = createResourceRoot(ExtensionSetup.class.getPackage(), TestExtension.class);
        Map<String, StreamExporter> content = Collections.singletonMap("test-extension.jar", exporter);
        support.addTestModule(TestExtension.MODULE_NAME, moduleXml, content);
    }

    public static void initializeTransformersExtension(final DomainTestSupport support) throws IOException {

        // slave - version1
        InputStream moduleXml = getModuleXml("transformers-module.xml");
        final StreamExporter version1 = createResourceRoot(ExtensionSetup.class.getPackage(), VersionedExtension1.class);
        Map<String, StreamExporter> v1 = Collections.singletonMap("transformers-extension.jar", version1);
        support.addOverrideModule("slave", VersionedExtensionCommon.EXTENSION_NAME, moduleXml, v1);

        // master - version2
        moduleXml = getModuleXml("transformers-module.xml");
        final StreamExporter version2 = createResourceRoot(ExtensionSetup.class.getPackage(), VersionedExtension2.class);
        Map<String, StreamExporter> v2 = Collections.singletonMap("transformers-extension.jar", version2);
        support.addOverrideModule("master", VersionedExtensionCommon.EXTENSION_NAME, moduleXml, v2);

    }

    static StreamExporter createResourceRoot(Package pkg, Class<? extends Extension> extensions) throws IOException {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class);
        archive.addPackage(ExtensionSetup.class.getPackage());
        archive.addAsServiceProvider(Extension.class, extensions);
        return archive.as(ZipExporter.class);
    }

    static InputStream getModuleXml(final String name) {
        // Get the module xml
        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        return tccl.getResourceAsStream("extension/" + name);
    }

}
