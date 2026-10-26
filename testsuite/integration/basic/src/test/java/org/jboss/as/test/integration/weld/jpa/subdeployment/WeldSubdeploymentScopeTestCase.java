/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.jpa.subdeployment;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * WFLY-6485
 * <p/>
 * Test that JPA dependencies are set for sub-deployments
 *
 * @author Scott Marlow
 */
@ExtendWith(ArquillianExtension.class)
public class WeldSubdeploymentScopeTestCase {

    @Deployment
    public static Archive<?> deploy() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "cdiPuScope.ear");
        WebArchive war = ShrinkWrap.create(WebArchive.class, "simple.war");
        war.addClasses(WeldSubdeploymentScopeTestCase.class, CdiJpaInjectingBean.class);
        war.addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "util.jar");
        jar.addAsManifestResource(WeldSubdeploymentScopeTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        war.addAsLibrary(jar);
        ear.addAsModule(war);
        return ear;
    }

    @Inject
    private CdiJpaInjectingBean bean;

    @Test
    public void testInjectedPersistenceContext() throws Exception {
        Assertions.assertNotNull(bean.entityManagerFactory(),
                "WFLY-6485 regression, injected EntityManagerFactory should not be null but is");
    }
}

