/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.arquillian.container.managed;

import javax.naming.Context;
import javax.naming.NamingException;

import junit.framework.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * AS7-3111 Ensures the JNDI Naming {@link Context} can be injected
 *
 * @author <a href="mailto:alr@jboss.org">Andrew Lee Rubinger</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class InjectJndiContextTestCase {

    /**
     * Test EJB deployment w/ remote binding
     */
    @Deployment(testable = false)
    public static JavaArchive createDeployment() throws Exception {
        return ShrinkWrap.create(JavaArchive.class,"myejb.jar").addClasses(EjbBean.class,EjbBusiness.class);
    }

    /**
     * {@link Context} to be injected
     */
    @ArquillianResource
    private Context jndiContext;
    
    private static final String JNDI_NAME = "ejb:/myejb//EjbBean!org.jboss.as.arquillian.container.managed.EjbBusiness";

    /**
     * AS7-3111
     */
    @Test
    public void shouldInjectJndiContext() throws NamingException {
        Assert.assertNotNull("AS7-3111: JNDI Context must be injected", jndiContext);
        // Attempt to look up the remote EJB
        final EjbBusiness ejb = (EjbBusiness)jndiContext.lookup(JNDI_NAME);
        Assert.assertNotNull("Could not look up datasource using supplied JNDI Context", ejb);
    }
}
