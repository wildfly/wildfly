/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.configuration.nonportablemode;

import jakarta.enterprise.inject.spi.Extension;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class NonPortableModeTest {

    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(WebArchive.class).addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml").addPackage(NonPortableModeTest.class.getPackage())
                .addAsServiceProvider(Extension.class, NonPortableExtension.class)
                .addAsManifestResource(NonPortableModeTest.class.getPackage(), "jboss-all.xml", "jboss-all.xml");
    }

    @Test
    public void testApplicationDeploys(NonPortableExtension extension) {
        // in the strict mode, the extension would fail to deploy
        Assert.assertNotNull(extension);
    }
}
