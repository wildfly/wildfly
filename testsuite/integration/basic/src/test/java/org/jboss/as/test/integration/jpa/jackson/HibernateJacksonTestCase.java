/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.jackson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.wildfly.testing.tools.deployments.DeploymentDescriptors.createPermissionsXmlAsset;

import java.lang.reflect.ReflectPermission;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.inject.Inject;

/**
 * Ensure that Hibernate's JSON SQL type can be used with Jackson.
 *
 * @author Scott Marlow
 * @author Yoann Rodiere
 */
@RunWith(Arquillian.class)
public class HibernateJacksonTestCase {

    private static final String ARCHIVE_NAME = "jpa_jacksontest";

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar")
                .addAsManifestResource(HibernateJacksonTestCase.class.getPackage(), "persistence.xml", "persistence.xml")
                .addPackage(HibernateJacksonTestCase.class.getPackage())
                .addAsManifestResource(createPermissionsXmlAsset(
                        // Permission required for Jackson serialization
                        new RuntimePermission("accessDeclaredMembers"),
                        new ReflectPermission("suppressAccessChecks")),
                        "permissions.xml");
    }

    @Inject
    private EmployeeRepository repo;

    @Test
    public void testJson() {
        Employee createdEmp = new Employee();
        createdEmp.setId(1);
        createdEmp.setName("Jason");
        createdEmp.setAddress("1 Main street");
        createdEmp.setJsonData(new JsonData(createdEmp.getName(), createdEmp.getAddress()));
        repo.create(createdEmp);

        Employee retrievedEmp = repo.get(createdEmp.getId());

        JsonData retrievedData = retrievedEmp.getJsonData();
        assertNotNull(retrievedData);
        assertEquals(createdEmp.getName(), retrievedData.getName());
        assertEquals(createdEmp.getAddress(), retrievedData.getAddress());

        // Check this was actually serialized/deserialized as json with Jackson
        var serializationSpy = retrievedData.getSerializationSpy();
        assertNotNull(serializationSpy);
        assertStartsWith("com.fasterxml.jackson.", serializationSpy.serializerInfo);
        assertStartsWith("com.fasterxml.jackson.", serializationSpy.deserializerInfo);
    }

    private static void assertStartsWith(String expectedStart, String actual) {
        String message = "'" + actual + "' should start with '" + expectedStart + "'";
        assertNotNull(message, actual);
        assertTrue(message, actual.startsWith(expectedStart));
    }

}
