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
package org.jboss.as.test.smoke.embedded.deployment.rar.AS7_1452;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="stefano.maestri@redhat.com">Stefano Maestri</a>
 *         Test casae for AS7-1452: Resource Adapter config-property value passed incorrectly
 */
@RunWith(Arquillian.class)
@Ignore(value = "disbled because it needs a different arquillian config. Not yet supported by current smoke tests. "  +
        "To run it just enable it and run \"mvn -Djboss.server.config.file.name=standalone-jca.xml -Dtest=AS7* test\" ")
public class AS7_1452TestCase {


    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
    @Deployment(order = 1, testable = true)
    public static ResourceAdapterArchive createDeployment() {
        String deploymentName = "as7_1452.rar";

        ResourceAdapterArchive raa =
                ShrinkWrap.create(ResourceAdapterArchive.class, deploymentName);
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, UUID.randomUUID().toString() + ".jar");
        ja.addClasses(ConfigPropertyResourceAdapter.class, ConfigPropertyManagedConnectionFactory.class,
                ConfigPropertyManagedConnection.class, ConfigPropertyConnectionFactory.class,
                ConfigPropertyManagedConnectionMetaData.class,
                ConfigPropertyConnectionFactoryImpl.class, ConfigPropertyConnection.class,
                ConfigPropertyConnectionImpl.class, ConfigPropertyAdminObjectImpl.class,
                ConfigPropertyAdminObjectInterface.class, AS7_1452TestCase.class);
        raa.addAsLibrary(ja);

        raa.addAsManifestResource("rar/" + deploymentName + "/META-INF/ra.xml", "ra.xml");

        return raa;
    }

    /**
     * Define the deployment
     * @return The deployment archive
     * @throws Exception in case of errors
     */
    /*@Deployment(order = 2)
       public static Descriptor createDescriptor() throws Exception
       {
          ClassLoader cl = Thread.currentThread().getContextClassLoader();
          InputStreamDescriptor isd = new InputStreamDescriptor("configproperty-ra.xml",
                                                                cl.getResourceAsStream("configproperty-ra.xml"));
          return isd;
       }
    */
    /**
     * CF
     */
    private static final String CF_JNDI_NAME = "java:jboss/ConfigPropertyConnectionFactory1";

    /**
     * AO
     */
    private static final String AO_JNDI_NAME = "java:jboss/ConfigPropertyAdminObjectInterface1";

    /**
     * Test config properties
     *
     * @throws Throwable Thrown if case of an error
     */
    @Test
    public void testConfigProperties() throws Throwable {


        Context ctx = new InitialContext();
        ConfigPropertyConnectionFactory connectionFactory = (ConfigPropertyConnectionFactory) ctx.lookup(CF_JNDI_NAME);



        assertNotNull(connectionFactory);

       ConfigPropertyAdminObjectInterface adminObject = (ConfigPropertyAdminObjectInterface) ctx.lookup(AO_JNDI_NAME);


        assertNotNull(adminObject);

        ConfigPropertyConnection connection = connectionFactory.getConnection();
        assertNotNull(connection);

        assertEquals("A", connection.getResourceAdapterProperty());
        assertEquals("B", connection.getManagedConnectionFactoryProperty());

        assertEquals("C", adminObject.getProperty());

        connection.close();

    }

}
