/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.mappedname;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that the mappedName attribute of {@link jakarta.ejb.EJB @EJB} and {@link jakarta.annotation.Resource @Resource} is
 * processed correctly.
 * <p/>
 *
 * @see https://issues.jboss.org/browse/AS7-900
 *      <p/>
 *      User: Jaikiran Pai
 */
@RunWith(Arquillian.class)
public class MappedNameInjectionTestCase {

    @Deployment
    public static WebArchive createSecondDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class);
        war.addClasses(MappedNameInjectionTestCase.class, MappedNameBean.class);
        war.addAsWebInfResource(MappedNameInjectionTestCase.class.getPackage(), "/web.xml","/web.xml");
        return war;
    }

    /**
     * Test that resources are injected when <code>mappedName</code> attribute is used with @Resource and @EJB annotations
     *
     * @throws Exception
     */
    @Test
    public void testResourceInjectionWithMappedName() throws Exception {
        final String jndiName = "java:module/" + MappedNameBean.class.getSimpleName() + "!" + MappedNameBean.class.getName();
        final MappedNameBean bean = (MappedNameBean) new InitialContext().lookup(jndiName);

        Assert.assertTrue("@Resource with mappedName wasn't injected", bean.isResourceWithMappedNameInjected());
        Assert.assertTrue("@Resource with lookup attribute wasn't injected", bean.isResourceWithLookupNameInjected());

        Assert.assertTrue("@EJB with mappedName wasn't injected", bean.isEJBWithMappedNameInjected());
        Assert.assertTrue("@EJB with lookup attribute wasn't injected", bean.isEJBWithLookupNameInjected());

    }
}
