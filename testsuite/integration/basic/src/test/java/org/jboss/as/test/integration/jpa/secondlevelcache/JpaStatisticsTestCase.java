/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.secondlevelcache;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Jakarta Persistence statistics test
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JpaStatisticsTestCase extends ContainerResourceMgmtTestBase {

    private static final String ARCHIVE_NAME = "JpaStatisticsTestCase";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(JpaStatisticsTestCase.class,
                Employee.class,
                Company.class,
                SFSB1.class,
                SFSB2LC.class
        );

        jar.addAsManifestResource(JpaStatisticsTestCase.class.getPackage(), "persistence.xml", "persistence.xml");

        return jar;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    protected <T> T rawLookup(String name, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup(name));
    }

    @Test
    public void testJpaStatistics() throws Exception {

        ModelNode op =  Util.createOperation(READ_RESOURCE_OPERATION,
            PathAddress.pathAddress(DEPLOYMENT, ARCHIVE_NAME + ".jar")
                .append(SUBSYSTEM, "jpa")
                .append("hibernate-persistence-unit", ARCHIVE_NAME + ".jar#mypc")
            );
        op.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(true);
        op.get(ModelDescriptionConstants.RECURSIVE).set(true);
        // ensure that the WFLY-10964 regression doesn't occur,
        // "org.hibernate.MappingException: Unknown entity: entity-update-count" was being thrown due to
        // a bug in the (WildFly) Hibernate integration code.  This causes Jakarta Persistence statistics to not be shown
        // in WildFly management console.
        ModelNode result = executeOperation(op);

        Assert.assertFalse("Subsystem is empty (result=" + result+")", result.keys().size() == 0);

    }


}
