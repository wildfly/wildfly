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
package org.jboss.as.osgi.service;

import java.util.Iterator;
import java.util.Map;

import junit.framework.Assert;

import org.jboss.as.osgi.parser.SubsystemState.OSGiModule;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author David Bosschaert
 */
public class AutoInstallIntegrationTest {
    @Test
    public void testAutoStartOrdering() throws StartException {
        ServiceName aSvc = ServiceName.parse("a1");
        ServiceName bSvc = ServiceName.parse("b2");
        ServiceName cSvc = ServiceName.parse("c3");
        ServiceName dSvc = ServiceName.parse("d4");

        OSGiModule aMod = Mockito.mock(OSGiModule.class);
        OSGiModule bMod = Mockito.mock(OSGiModule.class);
        OSGiModule cMod = Mockito.mock(OSGiModule.class);
        OSGiModule dMod = Mockito.mock(OSGiModule.class);

        Map<ServiceName, OSGiModule> map = AutoInstallIntegration.createPendingServicesMap();
        map.put(aSvc, aMod);
        map.put(bSvc, bMod);
        map.put(cSvc, cMod);
        map.put(dSvc, dMod);

        // The start order is determined by iterating over the keys,
        // so make sure that this order is the correct one...
        Iterator<ServiceName> it = map.keySet().iterator();
        Assert.assertEquals("a1", it.next().getSimpleName());
        Assert.assertEquals("b2", it.next().getSimpleName());
        Assert.assertEquals("c3", it.next().getSimpleName());
        Assert.assertEquals("d4", it.next().getSimpleName());
    }
}
