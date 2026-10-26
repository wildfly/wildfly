/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.jaxb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.wildfly.testing.tools.deployments.DeploymentDescriptors.createPermissionsXmlAsset;

import java.lang.reflect.ReflectPermission;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;

/**
 * Ensure that Hibernate's XML SQL type can be used with Jaxb.
 *
 * @author Scott Marlow
 * @author Yoann Rodiere
 */
@ExtendWith(ArquillianExtension.class)
public class HibernateJaxbTestCase {

    private static final String ARCHIVE_NAME = "jpa_jaxbtest";

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar")
                .addAsManifestResource(HibernateJaxbTestCase.class.getPackage(), "persistence.xml", "persistence.xml")
                .addPackage(HibernateJaxbTestCase.class.getPackage())
                .addAsManifestResource(createPermissionsXmlAsset(
                        // Permission required for Jaxb serialization
                        new RuntimePermission("accessDeclaredMembers"),
                        new ReflectPermission("suppressAccessChecks")),
                        "permissions.xml");
    }

    @Inject
    private EmployeeRepository repo;

    @Test
    public void testXml() {
        Employee createdEmp = new Employee();
        createdEmp.setId(2);
        createdEmp.setName("Jason");
        createdEmp.setAddress("1 Main street");
        createdEmp.setXmlData(new XmlData(createdEmp.getName(), createdEmp.getAddress()));
        repo.create(createdEmp);

        Employee retrievedEmp = repo.get(createdEmp.getId());

        XmlData retrievedData = retrievedEmp.getXmlData();
        assertNotNull(retrievedData);
        assertEquals(createdEmp.getName(), retrievedData.getName());
        assertEquals(createdEmp.getAddress(), retrievedData.getAddress());

        // Check this was actually serialized/deserialized as json with Jackson
        var serializationSpy = retrievedData.getSerializationSpy();
        assertNotNull(serializationSpy);
        assertStartsWith("org.glassfish.jaxb.", serializationSpy.serializerInfo);
        assertStartsWith("org.glassfish.jaxb.", serializationSpy.deserializerInfo);
    }

    private static void assertStartsWith(String expectedStart, String actual) {
        String message = "'" + actual + "' should start with '" + expectedStart + "'";
        assertNotNull(actual, message);
        assertTrue(actual.startsWith(expectedStart), message);
    }

}
