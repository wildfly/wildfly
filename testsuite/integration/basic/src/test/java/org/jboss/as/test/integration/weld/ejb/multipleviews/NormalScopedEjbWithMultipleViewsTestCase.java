/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.multipleviews;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * Tests that Session beans can be instantiated using the bean constructor, rather than
 * the default constructor
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class NormalScopedEjbWithMultipleViewsTestCase {

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
        jar.addPackage(NormalScopedEjbWithMultipleViewsTestCase.class.getPackage());
        jar.addAsManifestResource(new StringAsset(""), "beans.xml");
        return jar;
    }

    @Inject
    private MusicPlayer musicPlayer;

    @Inject
    private EntertainmentDevice entertainmentDevice;

    @Test
    public void testBothViewsReferToSameBeanInstance() {
        musicPlayer.setSongCount(10);
        Assert.assertEquals(10, entertainmentDevice.getSongCount());
    }
}
