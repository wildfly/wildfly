/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.packaging;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * Tests that war deployments work correctly even with multiple beans.xml files
 *
 * AS7-6737
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class WeldMultipleBeansXmlTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive jar = ShrinkWrap.create(WebArchive.class);
        jar.addPackage(WeldMultipleBeansXmlTestCase.class.getPackage());
        jar.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        jar.addAsResource(EmptyAsset.INSTANCE, "META-INF/beans.xml");
        return jar;
    }

    @Inject
    private ABean bean;

    @Test
    public void testAlternatives() {
        Assert.assertNotNull(bean);
    }


}
