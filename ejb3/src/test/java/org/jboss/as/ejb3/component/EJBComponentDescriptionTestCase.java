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
package org.jboss.as.ejb3.component;

import org.jboss.as.ee.component.AbstractComponentConfiguration;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.junit.Test;

import javax.ejb.TransactionManagementType;
import java.lang.reflect.Method;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class EJBComponentDescriptionTestCase {
    private static class TestBean {
        public void someMethod() {
        }
    }

    /**
     * If a bean is BMT then txAttrs won't be available.
     *
     * @throws Exception
     */
    @Test
    public void testNoTxAttrs() throws Exception {
        final EJBComponentConfiguration configuration = mock(EJBComponentConfiguration.class);
        when(configuration.getTransactionManagementType()).thenReturn(TransactionManagementType.BEAN);

        final EEModuleDescription eeModuleDescription = new EEModuleDescription("TestApp", "TestModule");
        final EjbJarDescription ejbJarDescription = new EjbJarDescription(eeModuleDescription);

        final EJBComponentDescription description = new EJBComponentDescription("Test", "TestBean", ejbJarDescription) {
            @Override
            public MethodIntf getMethodIntf(String viewClassName) {
                return MethodIntf.LOCAL;
            }

            @Override
            protected AbstractComponentConfiguration constructComponentConfiguration() {
                return configuration;
            }
        };
        Class<?> viewClass = TestBean.class;
        Method viewMethod = TestBean.class.getMethod("someMethod");
        Method componentMethod = null;
        description.processViewMethod(configuration, viewClass, viewMethod, componentMethod);
        // no NPE means pass
    }
}
