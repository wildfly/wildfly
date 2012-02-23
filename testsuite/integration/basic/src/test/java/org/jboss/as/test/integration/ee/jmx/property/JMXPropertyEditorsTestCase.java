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

package org.jboss.as.test.integration.ee.jmx.property;

import java.io.IOException;
import java.io.InputStream;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.osgi.testing.ManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author baranowb
 * 
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JMXPropertyEditorsTestCase {

    private static final String SAR_DEPLOMENT_NAME = "property-editors-beans";
    private static final String SAR_DEPLOMENT_FILE = SAR_DEPLOMENT_NAME + ".sar";

    @Deployment
    public static Archive<?> deployment() {

        // jar
        final JavaArchive jmxSAR = ShrinkWrap.create(JavaArchive.class, SAR_DEPLOMENT_FILE);
        jmxSAR.addClass(WithPropertiesMBean.class);
        jmxSAR.addClass(WithProperties.class);

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        jmxSAR.addAsManifestResource(
                tccl.getResource("org/jboss/as/test/integration/ee/jmx/property/sar/META-INF/jboss-service.xml"),
                "jboss-service.xml");
      //Dependency: org.jboss.common-core
        jmxSAR.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                StringBuffer dependencies = new StringBuffer();
                dependencies.append("org.jboss.common-core");
                builder.addManifestHeader("Dependencies", dependencies.toString());
                return builder.openStream();
            }
        });
        
        System.err.println(jmxSAR.toString(true));

        return jmxSAR;
    }

    
    private static MBeanServerConnection connection; 
    private static ObjectName oname;
    @Test
    public void testPropertiesValues() throws Exception {
         //just check if its not null, 
        String [] attributeNames = {"Boolean","Char","Byte","Short"
                ,"Integer","Long","Float","Double","AtomicBoolean"
                ,"AtomicInteger","AtomicLong","BigDecimal"};
        MBeanInfo info = connection.getMBeanInfo(oname);
        MBeanAttributeInfo[] infos = info.getAttributes();
        for(MBeanAttributeInfo i:infos)
        {
            System.err.println(i);
        }
        for(String attrbiuteName:attributeNames)
        {
            Object attributeValue = connection.getAttribute(oname, attrbiuteName);
            Assert.assertNotNull("Found null attribute value for '"+attrbiuteName+"'",attributeValue);
        }
    }

    static final String HOST = "localhost";
    static final int PORT = 1090;

    @BeforeClass
    public static void initialize() throws Exception {
        connection = getMBeanServerConnection();
        Assert.assertNotNull(connection);
        oname = new ObjectName("test:service=WithProperties");
    }

    @AfterClass
    public static void closeConnection() throws Exception {
        connection = null;
    }

    private static MBeanServerConnection getMBeanServerConnection() throws IOException {
        return JMXConnectorFactory.connect(new JMXServiceURL("service:jmx:remoting-jmx://localhost:9999"))
                .getMBeanServerConnection();

    }

}
