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
package org.jboss.as.demos.managedbean.runner;

import static org.jboss.as.protocol.StreamUtils.safeClose;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.jboss.as.demos.DeploymentUtils;
import org.jboss.as.demos.managedbean.archive.SimpleManagedBean;
import org.jboss.as.demos.managedbean.mbean.TestMBean;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ExampleRunner {

    public static void main(String[] args) throws Exception {
        DeploymentUtils utils = null;
        try {
            EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "managedbean-example.ear");
            JavaArchive sar = ShrinkWrap.create(JavaArchive.class, "managedbean-mbean.sar");
            sar.addManifestResource("archives/managedbean-mbean.sar/META-INF/MANIFEST.MF", "MANIFEST.MF");
            sar.addManifestResource("archives/managedbean-mbean.sar/META-INF/jboss-service.xml", "jboss-service.xml");
            sar.addPackage(TestMBean.class.getPackage());
            ear.add(sar, "/");

            JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "managedbean-example.jar");
            jar.addManifestResource("archives/managedbean-example.jar/META-INF/MANIFEST.MF", "MANIFEST.MF");
            jar.addManifestResource("archives/managedbean-example.jar/META-INF/services/org.jboss.msc.service.ServiceActivator",
                    "services/org.jboss.msc.service.ServiceActivator");
            jar.addManifestResource(EmptyAsset.INSTANCE, "beans.xml");
            jar.addPackage(SimpleManagedBean.class.getPackage());
            ear.add(jar, "/");

            utils = new DeploymentUtils(ear);

            utils.deploy();
            ObjectName objectName = new ObjectName("jboss:name=test,type=managedbean");
            MBeanServerConnection mbeanServer = utils.getConnection();
            System.out.println("Calling echo(\"Hello\")");
            Object o = mbeanServer.invoke(objectName, "echo", new Object[] { "Hello" }, new String[] { "java.lang.String" });
            System.out.println("echo returned " + o);
        } finally {
            utils.undeploy();
            safeClose(utils);
        }
    }

}
