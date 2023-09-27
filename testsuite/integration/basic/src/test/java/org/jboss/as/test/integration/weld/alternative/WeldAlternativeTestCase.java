/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.alternative;

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
 * A test of the Jakarta Contexts and Dependency Injection alternatives. This tests that the alternative
 * information in the beans.xml file is being parsed correctly.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class WeldAlternativeTestCase {

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
        jar.addPackage(WeldAlternativeTestCase.class.getPackage());
        jar.addAsManifestResource(new StringAsset("<beans bean-discovery-mode=\"all\"><alternatives><class>" + AlternativeBean.class.getName() + "</class></alternatives></beans>"), "beans.xml");
        return jar;
    }

    @Inject
    private SimpleBean bean;

    @Test
    public void testAlternatives() {
        Assert.assertEquals("Hello World", bean.sayHello());
    }


}
