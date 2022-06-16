/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.jboss.as.test.integration.weld.services;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.xml.namespace.QName;
import jakarta.xml.ws.Service;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 *
 * Tests that WeldResourceInjectionServices works correctly even in multithreaded scenarios.
 *
 * WFLY-9884
 *
 * @author Peter Mackay
 */
@RunWith(Arquillian.class)
@RunAsClient
public class WeldResourceInjectionServicesTestCase {

    @Deployment
    public static WebArchive getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "weldResourceInjectionServicesTest.war");
        war.addClasses(TestWS.class, TestWSImpl.class, ContextBean.class);
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    /**
     * Test that injecting a WebServiceContext into a bean used in a web service is working correctly
     * even in concurrent service calls.
     * See https://issues.jboss.org/browse/WFLY-9884 for details.
     */
    @Test
    public void testWebServiceContextInject(@ArquillianResource URL deploymentUrl) throws Exception {
        QName serviceName = new QName("http://www.jboss.org/jboss/as/test/TestWS", "TestService");
        URL wsdlUrl = new URL(deploymentUrl.toExternalForm() + "/" + TestWSImpl.SERVICE_NAME + "?wsdl");
        TestWS testService = Service.create(wsdlUrl, serviceName).getPort(TestWS.class);

        int workerCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        List<Future<String>> results = new ArrayList<>();
        try {
            for (int i = 0; i < workerCount; i++) {
                results.add(executor.submit(new Worker(testService)));
            }
            for (Future<String> result : results) {
                Assert.assertTrue(result.get(TimeoutUtil.adjust(5), TimeUnit.SECONDS).startsWith("Hello"));
            }
        } finally {
            executor.shutdown();
        }
    }

    private class Worker implements Callable<String> {

        TestWS service;

        private Worker(TestWS service) {
            this.service = service;
        }

        @Override
        public String call() {
            return service.sayHello();
        }
    }

}
