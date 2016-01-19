/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
 * Tests that the mappedName attribute of {@link javax.ejb.EJB @EJB} and {@link javax.annotation.Resource @Resource} is
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
