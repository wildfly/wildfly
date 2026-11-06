/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.configuration.nonportablemode;

import jakarta.enterprise.inject.spi.Extension;
import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ArquillianExtension.class)
public class NonPortableModeTest {

    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(WebArchive.class).addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml").addPackage(NonPortableModeTest.class.getPackage())
                .addAsServiceProvider(Extension.class, NonPortableExtension.class)
                .addAsManifestResource(NonPortableModeTest.class.getPackage(), "jboss-all.xml", "jboss-all.xml");
    }

    @Inject
    NonPortableExtension extension;

    @Test
    public void testApplicationDeploys() {
        // in the strict mode, the extension would fail to deploy
        Assertions.assertNotNull(extension);
    }
}
