/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jsr88.spi.beans;

import javax.enterprise.deploy.model.DDBean;
import javax.enterprise.deploy.spi.DConfigBean;
import javax.enterprise.deploy.spi.exceptions.ConfigurationException;

public class TestBeans {
    /*
     * public static void main( String args[] ) { //tempFunction1(); tempFunction2(); }
     *
     * public static void tempFunction2() {
     *
     * // DDBeanRootImpl impl = new DDBeanRootImpl(null, // new File("c:\\eclipse\\ConfigExample.xml"), "ConfigExample.xml"); //
     * DConfigBean configRoot1 = new JBossExample1ConfigBeanRoot(impl); // DConfigBean configRoot2 = new
     * JBossExample2ConfigBeanRoot(impl);
     *
     *
     * try { //traverse(configRoot1, impl, 0); //traverse(configRoot2, impl, 0); //test_remove(configRoot2, impl);
     * //test_getClass(); } catch( Exception e ) { System.out.println("Exception: " + e.getMessage()); e.printStackTrace(); } }
     *
     *
     * /* public static void test_getClass() { try { PackagedDeployable dep = new PackagedDeployable( new
     * File("c:\\eclipse\\crimeportal\\crimeportal.war")); WarConfiguration config = new WarConfiguration(dep);
     *
     * } catch( Exception e ) { System.out.println("DEAD: " + e.getMessage()); e.printStackTrace(); } }
     */

    public static void test_remove(DConfigBean config, DDBean dd) {
        try {
            System.out.println(config.getXpaths().length + " xpaths.");
            String targetXPath = config.getXpaths()[0];
            System.out.println(targetXPath + " is the first.");
            DDBean first = dd.getChildBean(targetXPath)[0];
            DConfigBean cnfg = config.getDConfigBean(first);
            System.out.println("cnfg has " + cnfg.getXpaths().length + " sub kids");
            config.removeDConfigBean(cnfg);
            System.out.println("cnfg has " + cnfg.getXpaths().length + " sub kids");
            System.out.println(config.getXpaths().length + " xpaths.");

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void traverse(DConfigBean config, DDBean dd, int indent) throws ConfigurationException {
        indent += 3;
        indentPrint(indent, "starting \"" + dd.getXpath() + "\", config of type " + trimClass(config.getClass()));
        String[] pathsToFollow = config.getXpaths();
        if (pathsToFollow.length > 0)
            indentPrint(indent, "- There are " + pathsToFollow.length + " xpaths returned.");
        indent += 4;
        for (int i = 0; i < pathsToFollow.length; i++) {
            String s = "path " + i + ": " + pathsToFollow[i];
            DDBean[] lesserBeans = dd.getChildBean(pathsToFollow[i]);
            indentPrint(indent, s + " , " + lesserBeans.length + " found.");

            for (int j = 0; j < lesserBeans.length; j++) {
                DConfigBean cb = config.getDConfigBean(lesserBeans[j]);
                traverse(cb, lesserBeans[j], indent);
            }
        }

    }

    public static String trimClass(Class c) {
        int dot = c.getName().lastIndexOf('.');
        int dollar = c.getName().lastIndexOf('$');
        if (dollar == -1) {
            return c.getName().substring(dot + 1);
        }
        return c.getName().substring(dollar + 1);
    }

    public static void indentPrint(int x, String y) {
        String s = "";
        for (int i = 0; i < x; i++)
            s += " ";
        System.out.println(s + y);
    }

}
