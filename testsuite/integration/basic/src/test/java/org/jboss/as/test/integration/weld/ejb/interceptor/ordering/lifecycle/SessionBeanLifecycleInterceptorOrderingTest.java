/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.weld.ejb.interceptor.ordering.lifecycle;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that session bean lifecycle interceptors are executed in the following order:
 *
* 1) Interceptors bound using the {@link Interceptors} annotation
* 2) Interceptors bound using CDI interceptor bindings
* 3) Interceptors defined on a superclass of the bean defining class
* 4) Interceptors defined on the bean defining class
 *
 *@see https://issues.jboss.org/browse/AS7-6739
 *
 * @author Jozef Hartinger
 *
 */
@RunWith(Arquillian.class)
public class SessionBeanLifecycleInterceptorOrderingTest {

    @Inject
    private BeanManager manager;

    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrap
                .create(WebArchive.class)
                .addPackage(SessionBeanLifecycleInterceptorOrderingTest.class.getPackage())
                .addAsWebInfResource(
                        new StringAsset("<beans><interceptors><class>" + CdiInterceptor.class.getName()
                                + "</class></interceptors></beans>"), "beans.xml");
    }

    @Test
    public void testSessionBeanLifecycleInterceptorOrdering() {
        @SuppressWarnings("unchecked")
        Bean<InterceptedBean> bean = (Bean<InterceptedBean>) manager.resolve(manager.getBeans(InterceptedBean.class));
        CreationalContext<InterceptedBean> ctx = manager.createCreationalContext(bean);

        List<String> expected = new ArrayList<String>();
        expected.add(LegacyInterceptor.class.getSimpleName());
        expected.add(CdiInterceptor.class.getSimpleName());
        expected.add(Superclass.class.getSimpleName());
        expected.add(InterceptedBean.class.getSimpleName());

        ActionSequence.reset();
        // create a new instance
        InterceptedBean instance = bean.create(ctx);
        assertEquals(expected, ActionSequence.getActions());

        ActionSequence.reset();
        bean.destroy(instance, ctx);
        assertEquals(expected, ActionSequence.getActions());
    }
}
