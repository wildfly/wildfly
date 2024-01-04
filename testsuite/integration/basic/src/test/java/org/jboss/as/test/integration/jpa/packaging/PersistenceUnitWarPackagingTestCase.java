/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.packaging;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.persistence.EntityManagerFactory;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that we are following the JPA 8.2.2 persistence unit scoping rules
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class PersistenceUnitWarPackagingTestCase {

    @Deployment
    public static Archive<?> deploy() {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "scopedToEar.ear");

        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        lib.addClasses(Employee.class);
        lib.addAsManifestResource(PersistenceUnitWarPackagingTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        ear.addAsLibrary(lib);

        WebArchive override = ShrinkWrap.create(WebArchive.class, "override.war");
        override.addClasses(Organisation.class, OrganisationBean.class, LibPersistenceUnitBean.class, PersistenceUnitWarPackagingTestCase.class);
        override.addAsResource(PersistenceUnitWarPackagingTestCase.class.getPackage(), "persistence.xml", "META-INF/persistence.xml");
        ear.addAsModule(override);

        JavaArchive noOverride = ShrinkWrap.create(JavaArchive.class, "noOverride.jar");
        noOverride.addClasses(EmployeeBean.class);
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
