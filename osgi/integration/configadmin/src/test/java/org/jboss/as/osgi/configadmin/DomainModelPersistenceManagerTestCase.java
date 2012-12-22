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
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.felix.cm.PersistenceManager;
import org.jboss.as.configadmin.service.ConfigAdminService;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
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

        ConfigAdminService mockJBCAS = Mockito.mock(ConfigAdminService.class);
        Set<String> pids = new HashSet<String>(Arrays.asList("a.b", "1"));
        Mockito.when(mockJBCAS.getConfigurations()).thenReturn(pids);

        ServiceController mockJBCASController = Mockito.mock(ServiceController.class);
        Mockito.when(mockJBCASController.getValue()).thenReturn(mockJBCAS);

        ServiceContainer mockServiceContainer = Mockito.mock(ServiceContainer.class);
        Mockito.when(mockServiceContainer.getRequiredService(ConfigAdminService.SERVICE_NAME)).thenReturn(mockJBCASController);

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

        // The OSGi ConfigAdmin needs to be primed with the configurations known to the JBoss ConfigAdmin
        Mockito.verify(mockOSGiCAS).getConfiguration("a.b", null);
        Mockito.verify(mockOSGiCAS).getConfiguration("1", null);
    }

    @Test
    public void testModificationFromDMRCausesUpdate() throws Exception {
        DomainModelPersistenceManager dmpm = new DomainModelPersistenceManager();
        Configuration mockConfiguration = initPersistenceManager(dmpm, "abc");

        Dictionary<String, String> dict = new Hashtable<String, String>();
        dict.put("a", "b");
        dict.put(ConfigAdminService.SOURCE_PROPERTY_KEY, ConfigAdminService.FROM_DMR_SOURCE_VALUE);
        dmpm.configurationModified("abc", dict);

        Mockito.verify(mockConfiguration).update(dict);
        Mockito.verifyNoMoreInteractions(mockConfiguration);
    }

    @Test
    public void testModificationNoUpdate() throws Exception {
        DomainModelPersistenceManager dmpm = new DomainModelPersistenceManager();
        Configuration mockConfiguration = initPersistenceManager(dmpm, "abc");

        Dictionary<String, String> dict = new Hashtable<String, String>();
        dict.put("a", "b");
        dmpm.configurationModified("abc", dict);

        // Because the modification is not marked to come from DMR it is assumed to come from the
        // OSGi Config Admin system, so it does not need to be fed back into it. Therefore the
        // Configuration.update() should not be called.
        Mockito.verify(mockConfiguration, Mockito.never()).update(dict);
        Mockito.verifyNoMoreInteractions(mockConfiguration);
    }

    @Test
    public void testConfigurationDeleted() throws Exception {
        DomainModelPersistenceManager dmpm = new DomainModelPersistenceManager();
        Configuration mockConfiguration = initPersistenceManager(dmpm, "abc");

        dmpm.configurationModified("abc", null);

        Mockito.verify(mockConfiguration).delete();
        Mockito.verifyNoMoreInteractions(mockConfiguration);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Configuration initPersistenceManager(DomainModelPersistenceManager dmpm, String pid) throws Exception {
        Configuration mockConfiguration = Mockito.mock(Configuration.class);

        ConfigurationAdmin mockOSGiCAS = Mockito.mock(ConfigurationAdmin.class);
        Mockito.when(mockOSGiCAS.getConfiguration(pid, null)).thenReturn(mockConfiguration);
        Mockito.when(mockOSGiCAS.listConfigurations("(" + Constants.SERVICE_PID + "=" + pid + ")")).
            thenReturn(new Configuration [] {mockConfiguration});

        ServiceReference mockCASSR = Mockito.mock(ServiceReference.class);
        ServiceReference mockSCSR = Mockito.mock(ServiceReference.class);

        ConfigAdminService mockJBCAS = Mockito.mock(ConfigAdminService.class);

        ServiceController mockJBCASController = Mockito.mock(ServiceController.class);
        Mockito.when(mockJBCASController.getValue()).thenReturn(mockJBCAS);

        ServiceContainer mockServiceContainer = Mockito.mock(ServiceContainer.class);
        Mockito.when(mockServiceContainer.getRequiredService(ConfigAdminService.SERVICE_NAME)).thenReturn(mockJBCASController);

        BundleContext mockBundleContext = Mockito.mock(BundleContext.class);
        Mockito.when(mockBundleContext.getServiceReferences(ConfigurationAdmin.class.getName(), null)).
        thenReturn(new ServiceReference [] {mockCASSR}); // call used by OSGi ServiceTracker
        Mockito.when(mockBundleContext.getServiceReference(ServiceContainer.class.getName())).thenReturn(mockSCSR);
        Mockito.when(mockBundleContext.getService(mockSCSR)).thenReturn(mockServiceContainer);
        Mockito.when(mockBundleContext.getService(mockCASSR)).thenReturn(mockOSGiCAS);

        dmpm.start(mockBundleContext);
        return mockConfiguration;
    }
}
