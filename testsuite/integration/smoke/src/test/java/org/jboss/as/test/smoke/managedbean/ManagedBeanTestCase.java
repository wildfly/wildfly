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
package org.jboss.as.test.smoke.managedbean;

import javax.naming.InitialContext;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.smoke.modular.utils.PollingUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
@RunWith(Arquillian.class)
public class ManagedBeanTestCase {

    @ArquillianResource
    private InitialContext context;

    @Deployment
    public static EnterpriseArchive createDeployment() throws Exception {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "managedbean-example.ear");


        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "managedbean-example.jar");
        jar.addAsManifestResource(new StringAsset("Manifest-Version: 1.0\n" +
                "Dependencies: org.jboss.as.ee,org.jboss.as.naming,org.jboss.msc,org.jboss.logging,javax.api"), "MANIFEST.MF");
        jar.addAsManifestResource(EmptyAsset.INSTANCE,"beans.xml");
        jar.addPackage(SimpleManagedBean.class.getPackage());
        jar.addPackage(ManagedBeanTestCase.class.getPackage());
        jar.addPackage(BeanWithSimpleInjected.class.getPackage());
        jar.addClass(PollingUtils.class);
        ear.add(jar, "/", ZipExporter.class);

        return ear;
    }

    @Test
    public void testManagedBean() throws Exception {
        BeanWithSimpleInjected bean = (BeanWithSimpleInjected) context.lookup("java:module/" + BeanWithSimpleInjected.class.getSimpleName());
        Assert.assertNotNull(bean);
        Assert.assertNotNull(bean.getSimple());
        String s = bean.echo("Hello");
        Assert.assertNotNull(s);
        Assert.assertEquals("#InterceptorFromParent##InterceptorBean##OtherInterceptorBean##BeanParent##BeanWithSimpleInjected#Hello#CDIBean#CDIBean", s);
    }

}
