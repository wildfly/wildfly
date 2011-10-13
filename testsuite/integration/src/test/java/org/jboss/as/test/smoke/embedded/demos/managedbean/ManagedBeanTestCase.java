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
package org.jboss.as.test.smoke.embedded.demos.managedbean;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.demos.managedbean.archive.BeanWithSimpleInjected;
import org.jboss.as.demos.managedbean.archive.LookupService;
import org.jboss.as.demos.managedbean.archive.SimpleManagedBean;
import org.jboss.as.test.smoke.modular.utils.PollingUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.UrlAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
@RunWith(Arquillian.class)
public class ManagedBeanTestCase {

    @Deployment
    public static EnterpriseArchive createDeployment() throws Exception {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "managedbean-example.ear");


        JavaArchive jar = ShrinkWrap.create(JavaArchive.class,"managedbean-example.jar");
        // AS7-1022: add as UrlAsset to workaround a Shrinkwrap bug
        final URL manifestURL = ManagedBeanTestCase.class.getClassLoader().getResource("archives/managedbean-example.jar/META-INF/MANIFEST.MF");
        jar.addAsManifestResource(new UrlAsset(manifestURL), "MANIFEST.MF");
        // AS7-1022: add as UrlAsset to workaround a Shrinkwrap bug
        final URL serviceActivatorURL = ManagedBeanTestCase.class.getClassLoader().getResource("archives/managedbean-example.jar/META-INF/services/org.jboss.msc.service.ServiceActivator");
        jar.addAsManifestResource(new UrlAsset(serviceActivatorURL), "services/org.jboss.msc.service.ServiceActivator");
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
        BeanWithSimpleInjected bean = LookupService.getBean();
        Assert.assertNotNull(bean);
        Assert.assertNotNull(bean.getSimple());
        String s = bean.echo("Hello");
        Assert.assertNotNull(s);
        Assert.assertEquals("#InterceptorBean##InterceptorFromParent##OtherInterceptorBean##BeanParent##BeanWithSimpleInjected#Hello#CDIBean#CDIBean", s);
    }

}
