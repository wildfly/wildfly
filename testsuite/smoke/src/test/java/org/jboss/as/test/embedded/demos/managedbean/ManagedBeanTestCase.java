/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.embedded.demos.managedbean;

import javax.naming.NamingException;

import junit.framework.Assert;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.demos.managedbean.archive.BeanWithSimpleInjected;
import org.jboss.as.demos.managedbean.archive.LookupService;
import org.jboss.as.test.modular.utils.PollingUtils;
import org.jboss.as.test.modular.utils.ShrinkWrapUtils;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
@RunWith(Arquillian.class)
@Run(RunModeType.IN_CONTAINER)
public class ManagedBeanTestCase {

    @Deployment
    public static JavaArchive createDeployment() throws Exception {
        JavaArchive archive = ShrinkWrapUtils.createJavaArchive("demos/managedbean-example.jar", ManagedBeanTestCase.class.getPackage(), BeanWithSimpleInjected.class.getPackage());
        archive.addClass(PollingUtils.class);
        return archive;
    }

    @Test
    public void testManagedBean() throws Exception {
        try {
            PollingUtils.retryWithTimeout(3000, new PollingUtils.Task() {
                public boolean execute() throws Exception {
                    return LookupService.bean != null;
                }
            });
            BeanWithSimpleInjected bean = LookupService.bean;
            Assert.assertNotNull(bean);
            Assert.assertNotNull(bean.getSimple());
            String s = bean.echo("Hello");
            Assert.assertNotNull(s);
            Assert.assertEquals("#InterceptorBean##OtherInterceptorBean##BeanParent##BeanWithSimpleInjected#Hello", s);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

}
