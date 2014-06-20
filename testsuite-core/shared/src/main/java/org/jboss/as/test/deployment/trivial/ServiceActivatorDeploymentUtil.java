/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.deployment.trivial;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;

/**
 * Utilities for using {@link org.jboss.as.test.deployment.trivial.ServiceActivatorDeployment}.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
public class ServiceActivatorDeploymentUtil {

    private static final Map<String, String> DEFAULT_MAP = Collections.singletonMap(ServiceActivatorDeployment.DEFAULT_SYS_PROP_NAME,
            ServiceActivatorDeployment.DEFAULT_SYS_PROP_VALUE);

    public static final PathAddress RESOURCE_ADDRESS = PathAddress.pathAddress(
            PathElement.pathElement("core-service", "platform-mbean"),
            PathElement.pathElement(ModelDescriptionConstants.TYPE, "runtime")
    );

    public static void createServiceActivatorDeployment(File destination) throws IOException {
        createServiceActivatorDeployment(destination, null);
    }

    public static void createServiceActivatorDeployment(File destination, Map<String, String> properties) throws IOException {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class);
        archive.addClass(ServiceActivatorDeployment.class);
        archive.addAsServiceProvider(ServiceActivator.class, ServiceActivatorDeployment.class);
        if (properties != null && properties.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> prop : properties.entrySet()) {
                sb.append(prop.getKey());
                sb.append('=');
                sb.append(prop.getValue());
                sb.append("\n");
            }
            archive.addAsManifestResource(new StringAsset("Dependencies: org.jboss.msc\n"), "MANIFEST.MF");
            archive.addAsResource(new StringAsset(sb.toString()), ServiceActivatorDeployment.PROPERTIES_RESOURCE);
        }

        archive.as(ZipExporter.class).exportTo(destination);
    }

    public static void validateProperties(ModelControllerClient client) throws IOException, MgmtOperationException {
        validateProperties(client, PathAddress.EMPTY_ADDRESS, DEFAULT_MAP);
    }

    public static void validateProperties(ModelControllerClient client, Map<String, String>  properties) throws IOException, MgmtOperationException {
        validateProperties(client, PathAddress.EMPTY_ADDRESS, properties);
    }

    public static void validateProperties(ModelControllerClient client, PathAddress baseAddress) throws IOException, MgmtOperationException {
        validateProperties(client, baseAddress, DEFAULT_MAP);
    }

    public static void validateProperties(ModelControllerClient client, PathAddress baseAddress, Map<String, String>  properties) throws IOException, MgmtOperationException {
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            ModelNode value = getPropertyValue(client, baseAddress, entry.getKey());
            Assert.assertTrue(entry.getKey() + " is not defined: " + value, value.isDefined());
            Assert.assertEquals(entry.getKey() + " has the wrong value", entry.getValue(), value.asString());
        }
    }

    public static void validateNoProperties(ModelControllerClient client) throws IOException, MgmtOperationException {
        validateNoProperties(client, PathAddress.EMPTY_ADDRESS, DEFAULT_MAP.keySet());
    }

    public static void validateNoProperties(ModelControllerClient client, Set<String> properties) throws IOException, MgmtOperationException {
        validateNoProperties(client, PathAddress.EMPTY_ADDRESS, properties);
    }

    public static void validateNoProperties(ModelControllerClient client, PathAddress baseAddress) throws IOException, MgmtOperationException {
        validateNoProperties(client, baseAddress, DEFAULT_MAP.keySet());
    }

    public static void validateNoProperties(ModelControllerClient client, PathAddress baseAddress, Set<String>  properties) throws IOException, MgmtOperationException {
        for (String prop : properties) {
            ModelNode value = getPropertyValue(client, baseAddress, prop);
            Assert.assertFalse(prop + " is defined: " + value, value.isDefined());
        }
    }

    private static ModelNode getPropertyValue(ModelControllerClient client, PathAddress baseAddress, String propertyName) throws IOException, MgmtOperationException {
        ModelNode op = Util.createEmptyOperation(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION, baseAddress.append(RESOURCE_ADDRESS));
        op.get(ModelDescriptionConstants.NAME).set("system-properties");

        ModelNode result = ManagementOperations.executeOperation(client, op);
        return result.get(propertyName);
    }

    private ServiceActivatorDeploymentUtil() {
        // prevent instantiation
    }
}
