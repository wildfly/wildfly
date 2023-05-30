/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.test.integration.jsf.phaselistener.injectiontarget.bean;

import java.net.URL;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.core.util.JacksonFeature;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Tomas Remes
 */
@RunWith(Arquillian.class)
@RunAsClient
public class InjectionBeanToPhaseListenerTest {

    @ArquillianResource
    URL url;

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "jsfphaseListenerBean.war");
        war.addPackage(InjectionBeanToPhaseListenerTest.class.getPackage());
        war.addAsWebInfResource(InjectionBeanToPhaseListenerTest.class.getPackage(), "web.xml", "web.xml");
        war.addAsWebInfResource(InjectionBeanToPhaseListenerTest.class.getPackage(), "faces-config.xml", "faces-config.xml");
        war.addAsWebResource(InjectionBeanToPhaseListenerTest.class.getPackage(), "home.xhtml", "home.xhtml");
        war.addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
        return war;
    }

    @Test
    public void test() throws Exception {
        try (Client client = ClientBuilder.newClient().register(JacksonFeature.class)) {
            WebTarget target = client.target(url.toExternalForm() + "home.jsf");
            Response response = target.request().get();
            String value = response.getHeaderString("X-WildFly");
            Assert.assertNotNull(value);
            Assert.assertEquals(value, SimpleBean.MESSAGE);
        }
    }

}
