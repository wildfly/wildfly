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

package org.jboss.as.test.integration.ee.injection.primitivehandling;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Migration test from EJB Testsuite (ejbthree-1629) to AS7 [JIRA JBQA-5483].
 *
 * Incorrect handling of primitives. The issue arises when there's a bean method
 * accepting a primitive type (double or float) and also has an annotation on the
 * method (ex: @TransactionAttribute). Furthermore, the bean needs to have a
 * corresponding entry (minimally ejb-name and ejb-class) in ejb-jar.xml, for this
 * issue to manifest.
 *
 * @author Jaikiran Pai, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class Ejb3DescriptorHandlerPrimitiveHandlingTestCase
{
    private static final Logger log = Logger.getLogger(Ejb3DescriptorHandlerPrimitiveHandlingTestCase.class);

    @ArquillianResource
    InitialContext ctx;

    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "primitive-handling.jar")
                .addPackage(Ejb3DescriptorHandlerPrimitiveHandlingTestCase.class.getPackage());
                jar.addAsManifestResource(Ejb3DescriptorHandlerPrimitiveHandlingTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        log.info(jar.toString(true));
        return jar;
    }

   /**
    * Test that the bean has been deployed and available for
    * method invocations.
    *
    * @throws Throwable
    */
    @Test
   public void testBeanInvocation() throws Throwable
   {
       String jndiName = "java:module/" + Ejb3DescriptorHandlerTestBean.class.getSimpleName() + "!" + Ejb3DescriptorHandlerTestRemote.class.getName();
      Object bean = ctx.lookup(jndiName);
      log.info("Successfully looked up the bean at " + jndiName);

      Assert.assertNotNull("Object returned from JNDI lookup for " + jndiName + " is null", bean);
      Assert.assertTrue("Object returned from JNDI lookup for " + jndiName + " is not an instance of "
            + Ejb3DescriptorHandlerTestRemote.class, (bean instanceof Ejb3DescriptorHandlerTestRemote));

      // Call the method on the bean
      Ejb3DescriptorHandlerTestRemote primitiveTesterBean = (Ejb3DescriptorHandlerTestRemote) bean;
      double someDouble = 2.0;
      double returnedDouble = primitiveTesterBean.doOpAndReturnDouble(someDouble);
      Assert.assertEquals("Bean returned unexpected value for primitive double",returnedDouble, someDouble, Double.NaN);

      // test on float
      float someFloat = 2.5F;
      float returnedFloat = primitiveTesterBean.doOpAndReturnFloat(someFloat);
      Assert.assertEquals("Bean returned unexpected value for primitive float",returnedFloat, someFloat, Float.NaN);

      // test on arrays
      float[] floatArray = new float[]{someFloat};
      float[] returnedFloatArray = primitiveTesterBean.doOpAndReturnFloat(floatArray);
      Assert.assertEquals("Bean returned unexpected value for primitive float array",returnedFloatArray.length, floatArray.length);
      Assert.assertEquals("Bean returned unexpected value for primitive float array contents",returnedFloatArray[0], floatArray[0], Float.NaN);

      double[] doubleArray = new double[]{someDouble};
      double[] returnedDoubleArray = primitiveTesterBean.doOpAndReturnDouble(doubleArray);
      Assert.assertEquals("Bean returned unexpected value for primitive double array",returnedDoubleArray.length, doubleArray.length);
      Assert.assertEquals("Bean returned unexpected value for primitive double array contents",returnedDoubleArray[0], doubleArray[0], Double.NaN);

      // now some simple method which says hi
      String name = "jai";
      String returnedMessage = primitiveTesterBean.sayHi(name);
      Assert.assertEquals("Bean returned unexpected message", returnedMessage, "Hi " + name);

   }

}
