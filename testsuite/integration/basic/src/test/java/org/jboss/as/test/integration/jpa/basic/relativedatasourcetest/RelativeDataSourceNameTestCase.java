/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.basic.relativedatasourcetest;

import jakarta.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.jpa.basic.Employee;
import org.jboss.as.test.integration.jpa.basic.SFSB1;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Transaction tests
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class RelativeDataSourceNameTestCase {

    private static final String ARCHIVE_NAME = "RelativeDataSourceNameTestCase";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(RelativeDataSourceNameTestCase.class,
                Employee.class,
                SFSB1.class
        );
        jar.addAsManifestResource(RelativeDataSourceNameTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        return jar;
    }

    @EJB(mappedName = "java:global/" + ARCHIVE_NAME + "/SFSB1")
    private SFSB1 sfsb1;

    @Test
    public void testQueryNonTXTransactionalEntityManagerInvocations() throws Exception {
        Exception error = null;
        sfsb1.createEmployee("Susan Sells", "1 Main Street", 1);
    }

}
