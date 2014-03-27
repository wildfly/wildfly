/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering.single.provider;

import static org.junit.Assert.*;
import static org.jboss.as.test.clustering.ClusteringTestConstants.*;

import java.util.Collection;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.EJBClientContextSelector;
import org.jboss.as.test.clustering.cluster.provider.bean.ServiceProviderRetriever;
import org.jboss.as.test.clustering.cluster.provider.bean.ServiceProviderRetrieverBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Validates that a service provider registration works in a non-clustered environment.
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ServiceProviderRegistrationTestCase {
    private static final Logger log = Logger.getLogger(ServiceProviderRegistrationTestCase.class);
    private static final String MODULE_NAME = "service-provider-registration";
    private static final String CLIENT_PROPERTIES = "cluster/ejb3/stateless/jboss-ejb-client.properties";

    @Deployment
    public static Archive<?> createDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejbJar.addPackage(ServiceProviderRetriever.class.getPackage());
        log.info(ejbJar.toString(true));
        return ejbJar;
    }

    @Test
    public void test() throws Exception {

        String cluster = "server";
        String nodeNameFormat = "%s/%s";
        String nodeName1 = String.format(nodeNameFormat, NODE_1, cluster);

        ContextSelector<EJBClientContext> selector = EJBClientContextSelector.setup(CLIENT_PROPERTIES);

        try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME)) {
            ServiceProviderRetriever bean = directory.lookupStateless(ServiceProviderRetrieverBean.class, ServiceProviderRetriever.class);
            Collection<String> names = bean.getProviders();
            assertEquals(1, names.size());
            assertTrue(names.toString(), names.contains(nodeName1));
        } finally {
            // reset the selector
            if (selector != null) {
                EJBClientContext.setSelector(selector);
            }
        }
    }
}
