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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.as.test.integration.management.extension.EmptySubsystemParser;
import org.jboss.as.test.integration.management.extension.blocker.BlockerExtension;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.StreamExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * @author Emanuel Muckenhuber
 */
public class ExtensionSetup {

    public static void initializeTestExtension(final DomainTestSupport support) throws IOException {
        // Get module.xml, create modules.jar and add to test config
        final InputStream moduleXml = getModuleXml("module.xml");
        StreamExporter exporter = createResourceRoot(TestExtension.class, ExtensionSetup.class.getPackage(), EmptySubsystemParser.class.getPackage());
        Map<String, StreamExporter> content = Collections.singletonMap("test-extension.jar", exporter);
        support.addTestModule(TestExtension.MODULE_NAME, moduleXml, content);
    }

    public static void addExtensionAndSubsystem(final DomainTestSupport support) throws IOException, MgmtOperationException {
        DomainClient masterClient = support.getDomainMasterLifecycleUtil().getDomainClient();
        PathAddress profileAddress = PathAddress.pathAddress("profile", "profile-a");

        PathAddress subsystemAddress = profileAddress.append("subsystem", "1");

        ModelNode addExtension = Util.createAddOperation(PathAddress.pathAddress("extension", TestExtension.MODULE_NAME));
        DomainTestUtils.executeForResult(addExtension, masterClient);

        ModelNode addSubsystem = Util.createAddOperation(subsystemAddress);
        addSubsystem.get("name").set("dummy name");
        DomainTestUtils.executeForResult(addSubsystem, masterClient);

        ModelNode addResource = Util.createAddOperation(subsystemAddress.append("rbac-sensitive","other"));
        DomainTestUtils.executeForResult(addResource, masterClient);

        addResource = Util.createAddOperation(subsystemAddress.append("rbac-constrained","default"));
        addResource.get("password").set("sa");
        addResource.get("security-domain").set("other");
        DomainTestUtils.executeForResult(addResource, masterClient);
    }

    public static void initializeTransformersExtension(final DomainTestSupport support) throws IOException {

        // slave - version1
        InputStream moduleXml = getModuleXml("transformers-module.xml");
        final StreamExporter version1 = createResourceRoot(VersionedExtension1.class, ExtensionSetup.class.getPackage(), EmptySubsystemParser.class.getPackage());
        Map<String, StreamExporter> v1 = Collections.singletonMap("transformers-extension.jar", version1);
        support.addOverrideModule("slave", VersionedExtensionCommon.EXTENSION_NAME, moduleXml, v1);

        // master - version2
        moduleXml = getModuleXml("transformers-module.xml");
        final StreamExporter version2 = createResourceRoot(VersionedExtension2.class, ExtensionSetup.class.getPackage());
        Map<String, StreamExporter> v2 = Collections.singletonMap("transformers-extension.jar", version2);
        support.addOverrideModule("master", VersionedExtensionCommon.EXTENSION_NAME, moduleXml, v2);

    }

    public static void initializeBlockerExtension(final DomainTestSupport support) throws IOException {
        // Get module.xml, create modules.jar and add to test config
        final InputStream moduleXml = getModuleXml("blocker-module.xml");
        StreamExporter exporter = createResourceRoot(BlockerExtension.class, EmptySubsystemParser.class.getPackage());
        Map<String, StreamExporter> content = Collections.singletonMap("blocker-extension.jar", exporter);
        support.addTestModule(BlockerExtension.MODULE_NAME, moduleXml, content);
    }

    static StreamExporter createResourceRoot(Class<? extends Extension> extension, Package... additionalPackages) throws IOException {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class);
        archive.addPackage(extension.getPackage());
        if (additionalPackages != null) {
            for (Package pkg : additionalPackages) {
                archive.addPackage(pkg);
            }
        }
        archive.addAsServiceProvider(Extension.class, extension);
        return archive.as(ZipExporter.class);
    }

    static InputStream getModuleXml(final String name) {
        // Get the module xml
        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        return tccl.getResourceAsStream("extension/" + name);
    }

}
