/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.preview.persistence.cdi;

import static org.junit.Assert.assertNotNull;

import jakarta.persistence.SchemaManager;
import jakarta.inject.Inject;


import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that CDI Persistence integration features works as described in
 * (web page) Persistence/CDI integration as mentioned in jakarta.ee/specifications/platform/11/jakarta-platform-spec-11.0#a441.
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class CDIPersistenceSchemaManagerTestCase {

    private static final String ARCHIVE_NAME = "CDIPersistenceSchemaManagerTestCase";

    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "CDIPersistenceSchemaManagerTestCase.jar");
        jar.addClasses(CDIPersistenceSchemaManagerTestCase.class, Employee.class, Pu1Qualifier.class, Pu2Qualifier.class, RequestScopedTestBean.class);
        jar.addAsManifestResource(CDIPersistenceSchemaManagerTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        jar.addAsManifestResource(CDIPersistenceSchemaManagerTestCase.class.getPackage(), "beans.xml", "beans.xml");
        return jar;
    }

    @Inject
    RequestScopedTestBean cmtBean;

    @Test
    public void TestSchemaManager() throws Exception {
        SchemaManager schemaManager = cmtBean.testSchemaManager();
        assertNotNull("SchemaManager should of been returned", schemaManager);

    }

}
