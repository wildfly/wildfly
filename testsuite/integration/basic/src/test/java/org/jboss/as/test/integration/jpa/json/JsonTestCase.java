/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.jpa.json;

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
 * Ensure that Hibernate JSON mapping can be used.
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class JsonTestCase {

    private static final String ARCHIVE_NAME = "jpa_jsontest";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(JsonTestCase.class,
                Employee.class,
                SFSB.class
        );
        jar.addAsManifestResource(JsonTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
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
