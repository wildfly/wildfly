/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.cdi;

import static org.junit.Assert.assertNull;

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
public class CDIPersistenceTestCase {

    private static final String ARCHIVE_NAME = "CDIPersistenceTestCase";

    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "CDIPersistenceTestCase.jar");
        jar.addClasses(CDIPersistenceTestCase.class, Employee.class, Pu1Qualifier.class, SFSB1.class);
        jar.addAsManifestResource(CDIPersistenceTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        jar.addAsManifestResource(CDIPersistenceTestCase.class.getPackage(), "beans.xml", "beans.xml");
        return jar;
    }

    @Inject
    SFSB1 cmtBean;

    @Test
    public void doCMTTest() throws Exception {

        Employee emp = cmtBean.getEmployeeExpectNullResult(101);
        assertNull("expected null result ", emp);
    }


}
