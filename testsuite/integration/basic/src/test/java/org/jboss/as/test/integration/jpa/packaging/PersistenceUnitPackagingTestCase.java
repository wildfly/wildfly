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

package org.jboss.as.test.integration.jpa.packaging;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that we are following the JPA 8.2.2 persistence unit scoping rules
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class PersistenceUnitPackagingTestCase {

    @Deployment
    public static Archive<?> deploy() {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "scopedToEar.ear");

        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        lib.addClasses(Employee.class);
        lib.addAsManifestResource(PersistenceUnitPackagingTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        ear.addAsLibrary(lib);

        JavaArchive override = ShrinkWrap.create(JavaArchive.class, "override.jar");
        override.addClasses(Organisation.class, OrganisationBean.class, LibPersistenceUnitBean.class);
        override.addAsManifestResource(PersistenceUnitPackagingTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        ear.addAsModule(override);

        JavaArchive noOverride = ShrinkWrap.create(JavaArchive.class, "noOverride.jar");
        noOverride.addClasses(EmployeeBean.class, PersistenceUnitPackagingTestCase.class);
        ear.addAsModule(noOverride);

        return ear;
    }

    @ArquillianResource
    private static InitialContext iniCtx;

    /**
     * As override.jar has it's own PU with the same name as the ear level PU then the local PU should be used
     */
    @Test
    public void testLocalPuDefinitionOverridesEarLibPu() throws NamingException {
        OrganisationBean bean = (OrganisationBean) iniCtx.lookup("java:app/override/OrganisationBean");
        validate(bean.getEntityManagerFactory(), Organisation.class, Employee.class);
        validate(bean.getDefaultEntityManagerFactory(), Organisation.class, Employee.class);
    }

    /**
     * noOverride.jar should be able to resolve the ear level pu
     */
    @Test
    public void testUsingEarLibPuInSubdeployment() throws NamingException {
        EmployeeBean bean = (EmployeeBean) iniCtx.lookup("java:app/noOverride/EmployeeBean");
        validate(bean.getEntityManagerFactory(), Employee.class, Organisation.class);
        validate(bean.getDefaultEntityManagerFactory(), Employee.class, Organisation.class);
    }


    @Test
    public void testUserOfOveriddenSubDeploymentUsingExplicitPath() throws NamingException {
        LibPersistenceUnitBean bean = (LibPersistenceUnitBean) iniCtx.lookup("java:app/override/LibPersistenceUnitBean");
        validate(bean.getEntityManagerFactory(), Employee.class, Organisation.class);
    }

    private static void validate(EntityManagerFactory emf, Class<?> entity, Class<?> notAnEntity) {
        emf.getMetamodel().entity(entity);
        try {
            emf.getMetamodel().entity(notAnEntity);
            Assert.fail(notAnEntity + " should not be an entity in this PU");
        } catch (IllegalArgumentException expected) {
        }
    }


}
