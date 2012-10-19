/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.osgi.configadmin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.felix.cm.PersistenceManager;
import org.jboss.as.configadmin.ConfigAdmin;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * @author David Bosschaert
 */
public class DomainModelPersistenceManagerTestCase {

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testInitialization() throws Exception {
        // Set up some mock objects
        ConfigurationAdmin mockOSGiCAS = Mockito.mock(ConfigurationAdmin.class);

        ServiceReference mockCASSR = Mockito.mock(ServiceReference.class);
        ServiceReference mockSCSR = Mockito.mock(ServiceReference.class);

        ConfigAdmin mockJBCAS = Mockito.mock(ConfigAdmin.class);
        Set<String> pids = new HashSet<String>(Arrays.asList("a.b", "1"));
        Mockito.when(mockJBCAS.getConfigurations()).thenReturn(pids);

        ServiceController mockJBCASController = Mockito.mock(ServiceController.class);
        Mockito.when(mockJBCASController.getValue()).thenReturn(mockJBCAS);

        ServiceContainer mockServiceContainer = Mockito.mock(ServiceContainer.class);
        Mockito.when(mockServiceContainer.getRequiredService(ConfigAdmin.SERVICE_NAME)).thenReturn(mockJBCASController);

        BundleContext mockBundleContext = Mockito.mock(BundleContext.class);
        Mockito.when(mockBundleContext.getServiceReferences(ConfigurationAdmin.class.getName(), null)).
            thenReturn(new ServiceReference [] {mockCASSR}); // call used by OSGi ServiceTracker
        Mockito.when(mockBundleContext.getServiceReference(ServiceContainer.class.getName())).thenReturn(mockSCSR);
        Mockito.when(mockBundleContext.getService(mockSCSR)).thenReturn(mockServiceContainer);
        Mockito.when(mockBundleContext.getService(mockCASSR)).thenReturn(mockOSGiCAS);

        DomainModelPersistenceManager dmpm = new DomainModelPersistenceManager();

        // Start the bundle
        dmpm.start(mockBundleContext);

        // Verify that the persistence manager was registered in the Service Registry.
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
        Mockito.verify(mockBundleContext).registerService(PersistenceManager.class.getName(), dmpm, props);
    }
}
