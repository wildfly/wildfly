/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.jsonb;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Ensure that Hibernate JSON mapping can be used with Jsonb.
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class HibernateJsonbTestCase {

    private static final String ARCHIVE_NAME = "jpa_jsonbtest";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(HibernateJsonbTestCase.class,
                Employee.class,
                SFSB.class
        );
        jar.addAsManifestResource(HibernateJsonbTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        return jar;
    }

    @Inject
    private SFSB sfsb1;

    /**
     * @throws Exception
     */
    @Test
    public void testJson() throws Exception {
        // tx1 will create the employee
        sfsb1.createEmployee("Json", "1 Main Street", 1);

        // non-tx2 will load the entity
        Employee emp = sfsb1.getEmployeeNoTX(1);

        List<String> list = emp.getJsonValue();
        assertTrue(list.contains("Json"));
        assertTrue(list.contains("1 Main Street"));
        assertFalse(list.contains("Absent value"));
    }

}
