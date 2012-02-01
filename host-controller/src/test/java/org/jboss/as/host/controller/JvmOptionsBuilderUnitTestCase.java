/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.host.controller;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class JvmOptionsBuilderUnitTestCase {

    @Test
    public void testNoOptionsSun() {
        testNoOptions(JvmType.SUN);
    }

    @Test
    public void testNoOptionsIbm() {
        testNoOptions(JvmType.IBM);
    }

    private void testNoOptions(JvmType type) {
        JvmElement element = JvmElementTestUtils.create(type);

        List<String> command = new ArrayList<String>();
        JvmOptionsBuilderFactory.getInstance().addOptions(element, command);

        Assert.assertEquals(0, command.size());
    }

    @Test
    public void testDebugOptionsNotEnabledSun() {
        testDebugOptionsNotEnabled(JvmType.SUN);
    }

    @Test
    public void testDebugOptionsNotEnabledIbm() {
        testDebugOptionsNotEnabled(JvmType.IBM);
    }

    private void testDebugOptionsNotEnabled(JvmType type) {
        JvmElement element = JvmElementTestUtils.create(type);
        JvmElementTestUtils.setDebugEnabled(element, false);
        JvmElementTestUtils.setDebugOptions(element, "-Xrunjdwp:transport=dt_socket,server=y,suspend=n");

        List<String> command = new ArrayList<String>();
        JvmOptionsBuilderFactory.getInstance().addOptions(element, command);

        Assert.assertEquals(0, command.size());
    }

    @Test
    public void testDebugOptionsAndEnabledSun() {
        testDebugOptionsAndEnabled(JvmType.SUN);
    }

    @Test
    public void testDebugOptionsAndEnabledIbm() {
        testDebugOptionsAndEnabled(JvmType.IBM);
    }

    private void testDebugOptionsAndEnabled(JvmType type) {
        JvmElement element = JvmElementTestUtils.create(type);
        JvmElementTestUtils.setDebugEnabled(element, true);
        JvmElementTestUtils.setDebugOptions(element, "-Xrunjdwp:transport=dt_socket,server=y,suspend=n");

        List<String> command = new ArrayList<String>();
        JvmOptionsBuilderFactory.getInstance().addOptions(element, command);

        Assert.assertEquals(1, command.size());
        Assert.assertTrue(command.contains("-Xrunjdwp:transport=dt_socket,server=y,suspend=n"));
    }

    @Test
    public void testHeapSun() {
        testHeap(JvmType.SUN);
    }

    @Test
    public void testHeapIbm() {
        testHeap(JvmType.IBM);
    }

    private void testHeap(JvmType type) {
        JvmElement element = JvmElementTestUtils.create(type);
        JvmElementTestUtils.setHeapSize(element, "28M");
        JvmElementTestUtils.setMaxHeap(element, "96M");

        List<String> command = new ArrayList<String>();
        JvmOptionsBuilderFactory.getInstance().addOptions(element, command);

        Assert.assertEquals(2, command.size());
        Assert.assertTrue(command.contains("-Xms28M"));
        Assert.assertTrue(command.contains("-Xmx96M"));
    }

    @Test
    public void testPermgenSun() {
        JvmElement element = JvmElementTestUtils.create(JvmType.SUN);
        JvmElementTestUtils.setPermgenSize(element, "32M");
        JvmElementTestUtils.setMaxPermgen(element, "64M");

        List<String> command = new ArrayList<String>();
        JvmOptionsBuilderFactory.getInstance().addOptions(element, command);

        Assert.assertEquals(2, command.size());
        Assert.assertTrue(command.contains("-XX:PermSize=32M"));
        Assert.assertTrue(command.contains("-XX:MaxPermSize=64M"));
    }

    @Test
    public void testPermgenIbm() {
        JvmElement element = JvmElementTestUtils.create(JvmType.IBM);
        JvmElementTestUtils.setPermgenSize(element, "32M");
        JvmElementTestUtils.setMaxPermgen(element, "64M");

        List<String> command = new ArrayList<String>();
        JvmOptionsBuilderFactory.getInstance().addOptions(element, command);

        Assert.assertEquals(0, command.size());
    }

    @Test
    public void testStackSun() {
        testStack(JvmType.SUN);
    }

    @Test
    public void testStackIbm() {
        testStack(JvmType.IBM);
    }

    private void testStack(JvmType type) {
        JvmElement element = JvmElementTestUtils.create(type);
        JvmElementTestUtils.setStack(element, "1M");

        List<String> command = new ArrayList<String>();
        JvmOptionsBuilderFactory.getInstance().addOptions(element, command);

        Assert.assertEquals(1, command.size());
        Assert.assertTrue(command.contains("-Xss1M"));
    }

    @Test
    public void testAgentLibSun() {
        testAgentLib(JvmType.SUN);
    }

    @Test
    public void testAgentLibIbm() {
        testAgentLib(JvmType.IBM);
    }

    private void testAgentLib(JvmType type) {
        JvmElement element = JvmElementTestUtils.create(type);
        JvmElementTestUtils.setAgentLib(element, "blah=x");

        List<String> command = new ArrayList<String>();
        JvmOptionsBuilderFactory.getInstance().addOptions(element, command);

        Assert.assertEquals(1, command.size());
        Assert.assertTrue(command.contains("-agentlib:blah=x"));
    }


    @Test
    public void testAgentPathSun() {
        testAgentPath(JvmType.SUN);
    }

    @Test
    public void testAgentPathIbm() {
        testAgentPath(JvmType.IBM);
    }

    private void testAgentPath(JvmType type) {
        JvmElement element = JvmElementTestUtils.create(type);
        JvmElementTestUtils.setAgentPath(element, "blah.jar=x");

        List<String> command = new ArrayList<String>();
        JvmOptionsBuilderFactory.getInstance().addOptions(element, command);

        Assert.assertEquals(1, command.size());
        Assert.assertTrue(command.contains("-agentpath:blah.jar=x"));
    }

    @Test
    public void testJavaagentSun() {
        testJavaagent(JvmType.SUN);
    }

    @Test
    public void testJavaagentIbm() {
        testJavaagent(JvmType.IBM);
    }

    private void testJavaagent(JvmType type) {
        JvmElement element = JvmElementTestUtils.create(type);
        JvmElementTestUtils.setJavaagent(element, "blah.jar=x");

        List<String> command = new ArrayList<String>();
        JvmOptionsBuilderFactory.getInstance().addOptions(element, command);

        Assert.assertEquals(1, command.size());
        Assert.assertTrue(command.contains("-javaagent:blah.jar=x"));
    }

    @Test
    public void testJvmOptionsSun() {
        testJvmOptions(JvmType.SUN);
    }

    @Test
    public void testJvmOptionsIbm() {
        testJvmOptions(JvmType.IBM);
    }

    private void testJvmOptions(JvmType type) {
        JvmElement element = JvmElementTestUtils.create(type);
        JvmElementTestUtils.addJvmOption(element, "-Xblah1=yes");
        JvmElementTestUtils.addJvmOption(element, "-Xblah2=no");

        List<String> command = new ArrayList<String>();
        JvmOptionsBuilderFactory.getInstance().addOptions(element, command);

        Assert.assertEquals(2, command.size());
        Assert.assertTrue(command.contains("-Xblah1=yes"));
        Assert.assertTrue(command.contains("-Xblah2=no"));
    }

    @Test
    public void testJvmOptionsIgnoredWhenInMainSchemaSun() {
        testJvmOptionsIgnoredWhenInMainSchema(JvmType.SUN);
    }

    @Test
    public void testJvmOptionsIgnoredWhenInMainSchemaIbm() {
        testJvmOptionsIgnoredWhenInMainSchema(JvmType.IBM);
    }

    private void testJvmOptionsIgnoredWhenInMainSchema(JvmType type) {
        JvmElement element = JvmElementTestUtils.create(type);

        //Main schema
        JvmElementTestUtils.setDebugEnabled(element, true);
        JvmElementTestUtils.setDebugOptions(element, "-Xrunjdwp:transport=dt_socket,server=y,suspend=n");
        JvmElementTestUtils.setHeapSize(element, "28M");
        JvmElementTestUtils.setMaxHeap(element, "96M");
        JvmElementTestUtils.setPermgenSize(element, "32M");
        JvmElementTestUtils.setMaxPermgen(element, "64M");
        JvmElementTestUtils.setStack(element, "1M");
        JvmElementTestUtils.setAgentLib(element, "blah=x");
        JvmElementTestUtils.setJavaagent(element, "blah.jar=x");
        //Options
        JvmElementTestUtils.addJvmOption(element, "-Xblah1=yes");
        JvmElementTestUtils.addJvmOption(element, "-Xblah2=no");
        //Ignored options
        JvmElementTestUtils.addJvmOption(element, "-Xrunjdwp:ignoreme");
        JvmElementTestUtils.addJvmOption(element, "-Xms1024M");
        JvmElementTestUtils.addJvmOption(element, "-Xmx1024M");
        JvmElementTestUtils.addJvmOption(element, "-XX:PermSize=1024M");
        JvmElementTestUtils.addJvmOption(element, "-XX:MaxPermSize=1024M");
        JvmElementTestUtils.addJvmOption(element, "-Xss100M");
        JvmElementTestUtils.addJvmOption(element, "-agentlib:other=x");
        JvmElementTestUtils.addJvmOption(element, "-javaagent:other.jar=x");

        List<String> command = new ArrayList<String>();
        JvmOptionsBuilderFactory.getInstance().addOptions(element, command);

        Assert.assertEquals(type == JvmType.SUN ? 10 : 8, command.size());
        Assert.assertTrue(command.contains("-Xrunjdwp:transport=dt_socket,server=y,suspend=n"));
        Assert.assertTrue(command.contains("-Xms28M"));
        Assert.assertTrue(command.contains("-Xmx96M"));
        if (type == JvmType.SUN) {
            Assert.assertTrue(command.contains("-XX:PermSize=32M"));
            Assert.assertTrue(command.contains("-XX:MaxPermSize=64M"));
        }
        Assert.assertTrue(command.contains("-Xss1M"));
        Assert.assertTrue(command.contains("-agentlib:blah=x"));
        Assert.assertTrue(command.contains("-javaagent:blah.jar=x"));
        Assert.assertTrue(command.contains("-Xblah1=yes"));
        Assert.assertTrue(command.contains("-Xblah2=no"));
    }
}
