/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.model.test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Internal use only.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ChildFirstClassLoader extends URLClassLoader {

    private final ClassLoader parent;
    private final List<Pattern> parentFirst;
    private final List<Pattern> childFirst;


    ChildFirstClassLoader(ClassLoader parent, List<Pattern> parentFirst, List<Pattern> childFirst, URL...urls) {
        super(urls, parent);
        assert parent != null : "Null parent";
        assert parentFirst != null : "Null parent first";
        assert childFirst != null : "Null child first";
        this.parent = parent;
        this.childFirst = childFirst;
        this.parentFirst = parentFirst;
    }

    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

        if (loadFromParentOnly(name)) {
            return parent.loadClass(name);
        }

        // First, check if the class has already been loaded
        Class<?> c = findLoadedClass(name);
        if (c == null) {
            try {
                c = findClass(name);
            } catch (ClassNotFoundException e) {

            }
            if (c == null) {
                c = parent.loadClass(name);
            }
            if (c == null) {
                findClass(name);
            }
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }


    @Override
    public URL getResource(String name) {
        URL url = findResource(name);
        if (url != null) {
            return url;
        }
        return super.getResource(name);
    }

    private boolean loadFromParentOnly(String className) {
        boolean parent = false;
        for (Pattern pattern : parentFirst) {
            if (pattern.matcher(className).matches()) {
                parent = true;
                break;
            }
        }

        if (parent) {
            for (Pattern pattern : childFirst) {
                if (pattern.matcher(className).matches()) {
                    return false;
                }
            }
        }
        return parent;
    }

    public static void main(String[] args) {
        Pattern pattern = Pattern.compile("org\\.jboss\\.as\\.core\\.model\\.adapter\\.common\\..*");
        System.out.println(pattern);
        String className = "org.jboss.as.core.model.adapter.common.BLah";
        System.out.println(className);

        System.out.println(pattern.matcher(className).matches());

    }
}




