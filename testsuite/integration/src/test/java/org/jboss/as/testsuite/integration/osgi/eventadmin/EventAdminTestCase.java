/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
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
package org.jboss.as.testsuite.integration.osgi.eventadmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * A test that deployes the EventAdmin and sends/receives messages on a topic.
 *
 * @author thomas.diesler@jboss.com
 * @since 08-Dec-2009
 */
@RunWith(Arquillian.class)
public class EventAdminTestCase {

    static String TOPIC = "org/jboss/test/osgi/example/event";

    @Inject
    public Bundle bundle;

    @Inject
    public BundleContext context;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-eventadmin");
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(EventAdmin.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testEventHandler() throws Exception {

        assertNotNull("Bundle injected", bundle);

        bundle.start();
        assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundle.getState());

        BundleContext context = bundle.getBundleContext();

        // Register the EventHandler
        Dictionary param = new Hashtable();
        param.put(EventConstants.EVENT_TOPIC, new String[] { TOPIC });
        TestEventHandler eventHandler = new TestEventHandler();
        context.registerService(EventHandler.class.getName(), eventHandler, param);

        // Send event through the the EventAdmin
        ServiceReference sref = context.getServiceReference(EventAdmin.class.getName());
        EventAdmin eventAdmin = (EventAdmin) context.getService(sref);
        eventAdmin.sendEvent(new Event(TOPIC, (Dictionary) null));

        // Verify received event
        assertEquals("Event received", 1, eventHandler.received.size());
        assertEquals(TOPIC, eventHandler.received.get(0).getTopic());
    }

    static class TestEventHandler implements EventHandler {
        List<Event> received = new ArrayList<Event>();

        public void handleEvent(Event event) {
            received.add(event);
        }
    }
}