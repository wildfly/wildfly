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
package org.jboss.as.configadmin.service;

import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Thomas.Diesler@jboss.com
 * @since 02-Oct-2012
 */
public class ConfigAdminStateTestCase {

    @Test
    public void testPutConfiguration() throws Exception {
        ConfigAdminState state = new ConfigAdminState();

        Dictionary<String, String> configA = new Hashtable<String, String>();
        configA.put("foo", "bar");
        Assert.assertTrue(state.put("pidA", configA, false));
        Assert.assertEquals("bar", state.get("pidA").get("foo"));

        Dictionary<String, String> configB = new Hashtable<String, String>();
        configB.put("foo", "baz");
        Assert.assertTrue(state.put("pidA", configB, false));
        Assert.assertEquals("baz", state.get("pidA").get("foo"));

        Assert.assertFalse(state.put("pidA", configA, false));
        Assert.assertEquals("baz", state.get("pidA").get("foo"));
    }

    @Test
    public void testRemoveConfiguration() throws Exception {
        ConfigAdminState state = new ConfigAdminState();

        Dictionary<String, String> configA = new Hashtable<String, String>();
        configA.put("foo", "bar");
        Assert.assertTrue(state.put("pidA", configA, false));
        Assert.assertEquals("bar", state.get("pidA").get("foo"));

        Assert.assertTrue(state.remove("pidA"));
        Assert.assertNull(state.get("pidA"));

        Assert.assertFalse(state.put("pidA", configA, false));
        Assert.assertNull(state.get("pidA"));
    }


    @Test
    public void testRollbackConfiguration() throws Exception {
        ConfigAdminState state = new ConfigAdminState();

        Dictionary<String, String> configA = new Hashtable<String, String>();
        configA.put("foo", "bar");
        Assert.assertTrue(state.put("pidA", configA, false));
        Assert.assertEquals("bar", state.get("pidA").get("foo"));

        Dictionary<String, String> configB = new Hashtable<String, String>();
        configB.put("foo", "baz");
        Assert.assertTrue(state.put("pidA", configB, false));
        Assert.assertEquals("baz", state.get("pidA").get("foo"));

        Assert.assertTrue(state.put("pidA", configA, true));
        Assert.assertEquals("bar", state.get("pidA").get("foo"));

        Assert.assertTrue(state.remove("pidA"));
        Assert.assertNull(state.get("pidA"));
    }
}
