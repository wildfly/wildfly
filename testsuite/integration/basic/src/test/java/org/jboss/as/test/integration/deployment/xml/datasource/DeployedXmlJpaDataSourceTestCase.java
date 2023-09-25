/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.deployment.xml.datasource;

import java.util.Set;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test deployment of -ds.xml files as Jakarta Persistence data sources
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class DeployedXmlJpaDataSourceTestCase {


    public static final String TEST_DS_XML = "test-ds.xml";
    public static final String JPA_DEPLOYMENT_NAME = "jpaDeployment";


    @Deployment(name = JPA_DEPLOYMENT_NAME)
    public static Archive<?> deployJpa() {
        return ShrinkWrap.create(JavaArchive.class, JPA_DEPLOYMENT_NAME + ".jar")
                .addClasses(Employee.class, JpaRemoteBean.class,
                        JpaRemote.class, DeployedXmlJpaDataSourceTestCase.class)
                .addAsManifestResource(DeployedXmlJpaDataSourceTestCase.class.getPackage(),
                        "MANIFEST.MF", "MANIFEST.MF")
                .addAsManifestResource(DeployedXmlJpaDataSourceTestCase.class.getPackage(),
                        "persistence.xml", "persistence.xml")
                .addAsManifestResource(DeployedXmlJpaDataSourceTestCase.class.getPackage(),
                        "jpa-ds.xml", "jpa-ds.xml");
    }

    @ArquillianResource
    private InitialContext initialContext;

    @Test
    public void testJpaUsedWithXMLXaDataSource() throws Throwable {
        final JpaRemote remote = (JpaRemote) initialContext.lookup("java:module/JpaRemoteBean");
        remote.addEmployee("Bob");
        remote.addEmployee("Sue");
        final Set<String> emps = remote.getEmployees();
        Assert.assertEquals(2, emps.size());
        Assert.assertTrue(emps.contains("Bob"));
        Assert.assertTrue(emps.contains("Sue"));
    }


}
