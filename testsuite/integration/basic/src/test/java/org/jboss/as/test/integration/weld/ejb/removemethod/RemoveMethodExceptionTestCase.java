/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.removemethod;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.ejb.NoSuchEJBException;
import jakarta.inject.Inject;

/**
 * AS7-1463
 * <p/>
 * Tests that @Remove method for CDI managed beans throw exceptions.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class RemoveMethodExceptionTestCase {

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
        jar.addPackage(RemoveMethodExceptionTestCase.class.getPackage());
        jar.addAsManifestResource(new StringAsset(""), "beans.xml");
        return jar;
    }

    @Inject
    private House house;

    @Inject
    private Garage garage;

    @Test
    public void testRemoveMethodOnNormalScopedBean() {
        try {
            house.remove();
            throw new RuntimeException("Expected exception");
        } catch (UnsupportedOperationException e) {

        }
    }

    @Test
    public void testRemoveMethodOnDependentScopedCdiBean() {
        try {
            garage.remove();
            garage.park();
        } catch (Exception e) {
            if (!(e instanceof NoSuchEJBException)){
                Assert.fail("Expected NoSuchEJBException but got: " + e);
            }
            return;
        }
        Assert.fail("NoSuchEJBException should occur but didn't!");
    }
}
