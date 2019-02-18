/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ejb3.iiop;

import org.jboss.classfilewriter.ClassFactory;
import org.jboss.modules.ClassDefiner;
import org.jboss.modules.Module;

import java.security.ProtectionDomain;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WildFlyClassFactory implements ClassFactory {

    public static final ClassFactory INSTANCE = new WildFlyClassFactory();

    private WildFlyClassFactory() {
        // forbidden instantiation
    }

    @Override
    public Class<?> defineClass(final ClassLoader classLoader, final String name, final byte[] b, final int off, final int len, final ProtectionDomain protectionDomain) throws ClassFormatError {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            final int index = name.lastIndexOf('.');
            final String packageName;
            if(index == -1 ) {
                packageName = "";
            } else {
                packageName = name.substring(0, index);
            }
            RuntimePermission permission = new RuntimePermission("defineClassInPackage." + packageName);
            sm.checkPermission(permission);
        }
        final Module module = Module.forClassLoader(classLoader, false);
        return ClassDefiner.getInstance().defineClass(module, name, protectionDomain, b, off, len);
    }

}
