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
package org.jboss.as.osgi.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.jboss.modules.ModuleIdentifier;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author David Bosschaert
 */
public class SubsystemStateTestCase {

    @Test
    public void testConfiguration() {
        String pid = "myPid";
        SubsystemState state = new SubsystemState();

        final List<Observable> observables = new ArrayList<Observable>();
        final List<Object> arguments = new ArrayList<Object>();
        Observer o = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                observables.add(o);
                arguments.add(arg);
            }
        };
        state.addObserver(o);

        Assert.assertFalse(state.hasConfiguration(pid));
        Assert.assertNull(state.getConfiguration(pid));
        Assert.assertEquals(0, state.getConfigurations().size());

        Dictionary<String, String> d = new Hashtable<String, String>();

        d.put("a", "b");
        Assert.assertEquals("Precondition", 0, arguments.size());
        state.putConfiguration(pid, d);
        Assert.assertEquals(1, observables.size());
        Assert.assertEquals(1, arguments.size());
        Assert.assertSame(state, observables.get(0));

        SubsystemState.ChangeEvent event = (SubsystemState.ChangeEvent) arguments.get(0);
        assertEventEquals(pid, false, SubsystemState.ChangeType.CONFIG, event);

        Assert.assertEquals(map(d), map(state.getConfiguration(pid)));
        Assert.assertEquals(1, state.getConfigurations().size());
        Assert.assertEquals(pid, state.getConfigurations().iterator().next());

        Assert.assertEquals(map(d), map(state.removeConfiguration(pid)));
        Assert.assertEquals(2, observables.size());
        Assert.assertEquals(2, arguments.size());
        Assert.assertSame(state, observables.get(1));

        SubsystemState.ChangeEvent event2 = (SubsystemState.ChangeEvent) arguments.get(1);
        assertEventEquals(pid, true, SubsystemState.ChangeType.CONFIG, event2);
    }

    @Test
    public void testProperties() {
        SubsystemState state = new SubsystemState();

        final List<Observable> observables = new ArrayList<Observable>();
        final List<Object> arguments = new ArrayList<Object>();
        Observer o = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                observables.add(o);
                arguments.add(arg);
            }
        };
        state.addObserver(o);

        Assert.assertEquals("Precondition", Collections.emptyMap(), state.getProperties());

        Assert.assertEquals("Precondition", 0, arguments.size());
        Assert.assertNull(state.setProperty("a", "aaa"));
        Assert.assertNull(state.setProperty("b", "bbb"));
        Assert.assertEquals("bbb", state.setProperty("b", "ccc"));

        Assert.assertEquals(3, observables.size());
        Assert.assertEquals(Collections.nCopies(3, state), observables);
        Assert.assertEquals(3, arguments.size());

        SubsystemState.ChangeEvent event = (SubsystemState.ChangeEvent) arguments.get(0);
        assertEventEquals("a", false, SubsystemState.ChangeType.PROPERTY, event);
        SubsystemState.ChangeEvent event2 = (SubsystemState.ChangeEvent) arguments.get(1);
        assertEventEquals("b", false, SubsystemState.ChangeType.PROPERTY, event2);
        SubsystemState.ChangeEvent event3 = (SubsystemState.ChangeEvent) arguments.get(2);
        assertEventEquals("b", false, SubsystemState.ChangeType.PROPERTY, event3);

        Assert.assertEquals("aaa", state.getProperties().get("a"));
        Assert.assertEquals("ccc", state.getProperties().get("b"));

        Assert.assertEquals("aaa", state.setProperty("a", null));
        Assert.assertEquals(4, observables.size());
        Assert.assertEquals(Collections.nCopies(4, state), observables);
        Assert.assertEquals(4, arguments.size());
        SubsystemState.ChangeEvent event4 = (SubsystemState.ChangeEvent) arguments.get(3);
        assertEventEquals("a", true, SubsystemState.ChangeType.PROPERTY, event4);

        Assert.assertNull(state.getProperties().get("a"));
        Assert.assertEquals("ccc", state.getProperties().get("b"));
    }

    @Test
    public void testModules() {
        SubsystemState state = new SubsystemState();

        final List<Observable> observables = new ArrayList<Observable>();
        final List<Object> arguments = new ArrayList<Object>();
        Observer o = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                observables.add(o);
                arguments.add(arg);
            }
        };
        state.addObserver(o);

        Assert.assertEquals("Precondition", 0, state.getCapabilities().size());

        Assert.assertEquals("Precondition", 0, arguments.size());
        ModuleIdentifier id = ModuleIdentifier.fromString("hi");
        SubsystemState.OSGiCapability m = new SubsystemState.OSGiCapability(id, 3);
        state.addCapability(m);

        Assert.assertEquals(1, arguments.size());
        SubsystemState.ChangeEvent event = (SubsystemState.ChangeEvent) arguments.get(0);
        assertEventEquals(id.toString(), false, SubsystemState.ChangeType.CAPABILITY, event);

        Assert.assertEquals(Collections.singletonList(m), state.getCapabilities());

        Assert.assertNull(state.removeCapability("abc"));
        Assert.assertEquals(Collections.singletonList(m), state.getCapabilities());

        Assert.assertEquals(m, state.removeCapability("hi"));

        Assert.assertEquals(2, arguments.size());
        SubsystemState.ChangeEvent event2 = (SubsystemState.ChangeEvent) arguments.get(1);
        assertEventEquals(id.toString(), true, SubsystemState.ChangeType.CAPABILITY, event2);

        Assert.assertEquals(0, state.getCapabilities().size());
    }

    @Test
    public void testActivation() {
        SubsystemState state = new SubsystemState();

        final List<Observable> observables = new ArrayList<Observable>();
        final List<Object> arguments = new ArrayList<Object>();
        Observer o = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                observables.add(o);
                arguments.add(arg);
            }
        };
        state.addObserver(o);

        Assert.assertEquals("Default", SubsystemState.Activation.LAZY, state.getActivationPolicy());

        Assert.assertEquals("Precondition", 0, arguments.size());
        state.setActivation(SubsystemState.Activation.LAZY);
        Assert.assertEquals(0, arguments.size());

        state.setActivation(SubsystemState.Activation.EAGER);
        Assert.assertEquals(1, arguments.size());

        SubsystemState.ChangeEvent event = (SubsystemState.ChangeEvent) arguments.get(0);
        assertEventEquals(SubsystemState.Activation.EAGER.toString(), false, SubsystemState.ChangeType.ACTIVATION, event);
    }

    private void assertEventEquals(String id, boolean isRemoved, SubsystemState.ChangeType type, SubsystemState.ChangeEvent event) {
        Assert.assertEquals(id, event.getId());
        Assert.assertEquals(isRemoved, event.isRemoved());
        Assert.assertEquals(type, event.getType());
    }

    private Map<String, String> map(Dictionary<String, String> d) {
        Map<String, String> m = new HashMap<String, String>();

        for (Enumeration<String> e = d.keys(); e.hasMoreElements(); ) {
            String key = e.nextElement();
            m.put(key, d.get(key));
        }

        return m;
    }
}
