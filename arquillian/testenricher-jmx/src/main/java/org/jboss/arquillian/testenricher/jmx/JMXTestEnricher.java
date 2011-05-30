/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.testenricher.jmx;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.inject.Inject;

import org.jboss.arquillian.api.DeploymentProvider;
import org.jboss.arquillian.protocol.jmx.ResourceCallbackHandler;
import org.jboss.arquillian.protocol.jmx.ResourceCallbackHandlerAssociation;
import org.jboss.arquillian.spi.Context;
import org.jboss.arquillian.spi.TestClass;
import org.jboss.arquillian.spi.TestEnricher;

/**
 * The JMX TestEnricher
 *
 * The enricher supports the injection of the {@link DeploymentProvider}.
 *
 * <pre><code>
 *    @Inject
 *    DeploymentProvider provider;
 * </code></pre>
 *
 * @author thomas.diesler@jboss.com
 */
public class JMXTestEnricher implements TestEnricher
{
   @Override
   public void enrich(Context context, Object testCase)
   {
      Class<?> testClass = testCase.getClass();
      for (Field field : testClass.getDeclaredFields())
      {
         if (field.isAnnotationPresent(Inject.class))
         {
            if (field.getType().isAssignableFrom(DeploymentProvider.class))
            {
               injectDeploymentProvider(context, testCase, field);
            }
         }
      }
   }

   @Override
   public Object[] resolve(Context context, Method method)
   {
      return null;
   }

   private void injectDeploymentProvider(Context context, Object testCase, Field field)
   {
      try
      {
         TestClass testClass = new TestClass(testCase.getClass());
         field.set(testCase, getDeploymentProvider(context, testClass));
      }
      catch (IllegalAccessException ex)
      {
         throw new IllegalStateException("Cannot inject DeploymentProvider", ex);
      }
   }

   private DeploymentProvider getDeploymentProvider(Context context, TestClass testClass)
   {
      ResourceCallbackHandler callbackHandler = ResourceCallbackHandlerAssociation.getCallbackHandler();
      return new DeploymentProviderImpl(testClass, callbackHandler);
   }
}
