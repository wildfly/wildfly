/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.context.application.event;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.Testable;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
//import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testcase for WFLY-3334
 * @author Jozef Hartinger
 *
 */
@RunWith(Arquillian.class)
public class ApplicationContextInitializedEventTestCase {

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive lib = ShrinkWrap.create(JavaArchive.class)
                .addAsManifestResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml")
                .addClasses(Library.class, ApplicationContextInitializedEventTestCase.class);
        JavaArchive ejb = Testable.archiveToTest(ShrinkWrap.create(JavaArchive.class).addAsManifestResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml").addClass(SessionBean.class));
        return ShrinkWrap.create(EnterpriseArchive.class).addAsModule(ejb).addAsLibrary(lib);
    }

    @Test
    public void testEjbJar() {
        Assert.assertNotNull(SessionBean.EVENT);
    }

    @Test
    public void testLibrary() {
        Assert.assertNotNull(Library.EVENT);
    }
}
