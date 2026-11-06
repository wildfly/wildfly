/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.multideployment;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that Jakarta Contexts and Dependency Injection beans defined in a bundled library can be used in a deployment
 *
 * @author Jozef Hartinger
 *
 */
@ExtendWith(ArquillianExtension.class)
public class WeldBundledLibraryDeploymentTestCase extends AbstractBundledLibraryDeploymentTestCase {

    @Inject
    private SimpleBean bean;

    @Test
    public void testSimpleBeanInjected() {
        Assertions.assertNotNull(bean);
        bean.ping();
    }

    @Deployment
    public static Archive<?> getDeployment() throws Exception {
        doSetup();
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
        jar.addClasses(WeldBundledLibraryDeploymentTestCase.class, AbstractBundledLibraryDeploymentTestCase.class);
        jar.addAsManifestResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
        jar.setManifest(new StringAsset("Extension-List: weld1\nweld1-Extension-Name: " + EXTENSION_NAME + "\n"));
        return jar;
    }
}
