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

package org.jboss.as.web;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.catalina.Valve;
import org.jboss.modules.ModuleClassLoader;

/**
 * A Simple wrapper for Valves (Engine & Host)
 *
 * @author Jean-Frederic Clere
 */

public class WebValve {
    public static Valve createValve(String filename, String classname, ClassLoader parent) throws InstantiationException,
            IllegalAccessException, ClassNotFoundException, MalformedURLException {

        URLClassLoader child = new URLClassLoader(new URL[] { new URL("file:////" + filename) }, parent);
        Valve valve = (Valve) child.loadClass(classname).newInstance();
        return valve;

    }

    public static Valve createValve(String classname, ModuleClassLoader classLoader) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Valve valve = (Valve) classLoader.loadClass(classname).newInstance();
        return valve;
    }
}
