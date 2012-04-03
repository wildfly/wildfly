/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jpa.descriptor.location.ear;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
/**
 * Tests that the <jar-file> element of persistence.xml works as expected.
 * 
 * @author baranowb
 */
@RunWith(Arquillian.class)
public class EarFileTestCase {

    private static final String ARCHIVE_NAME = "jpajarfile.ear";

    @ArquillianResource
    Deployer deployer;

    @Deployment(managed = false, name = JpaDeployableSlsb.PU_NAME)
    public static Archive<?> deployableArchive() {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME);
        JavaArchive ejbModule = ShrinkWrap.create(JavaArchive.class, "my-ejb-module.jar");
        ejbModule.addClasses(EarFileTestCase.class, JpaDeployableSlsb.class);
        ejbModule.addAsManifestResource(getPersistenceXml(JpaDeployableSlsb.PU_NAME), "persistence.xml");
        ear.addAsModule(ejbModule);
        return ear;
    }

    @Deployment(managed = false, name = JpaNotDeployableSlsb.PU_NAME)
    public static Archive<?> undeployableArchive() {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME);
        JavaArchive ejbModule = ShrinkWrap.create(JavaArchive.class, "my-ejb-module.jar");
        ejbModule.addClasses(EarFileTestCase.class, JpaNotDeployableSlsb.class);
        ear.addAsManifestResource(getPersistenceXml(JpaNotDeployableSlsb.PU_NAME), "persistence.xml");
        ear.addAsModule(ejbModule);
        ear.addAsManifestResource(getPersistenceXml(JpaNotDeployableSlsb.PU_NAME), "persistence.xml");
        return ear;
    }

    @Test
    public void testDeployable() throws Exception {
        try {
            deployer.deploy(JpaDeployableSlsb.PU_NAME);
        } finally {
            try {
                deployer.undeploy(JpaDeployableSlsb.PU_NAME);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testUnDeployable() throws Exception {
        try {
            deployer.deploy(JpaNotDeployableSlsb.PU_NAME);
            fail();
        } catch(Exception e){
            if(e instanceof DeploymentException){
                return;
            }
                throw e;
        
        }finally {
        
        }
    }
    
    private static StringAsset getPersistenceXml(String puName) {
        return new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?> "
                + "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">"
                + "  <persistence-unit name=\"" + puName + "\">"
                + "  <jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>"
                + "<properties> <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/>" + "</properties>"
                + "  </persistence-unit>" + "</persistence>");
    }

}
