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

package org.jboss.as.test.compat.common;

import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * @author Scott Marlow
 */
public class JndiUtil {

    public static <T, U> U lookup(final InitialContext initialContext, final String archiveName, final Class<T> beanClass, final Class<U> interfaceClass) throws NamingException {
        try {
            return interfaceClass.cast(initialContext.lookup("java:global/" + archiveName + "/" + "beans/" + beanClass.getSimpleName() + "!" + interfaceClass.getName()));
        } catch (NamingException e) {
            JndiUtil.dumpJndi(initialContext, "");
            throw e;
        }
    }

    public static <T> T rawLookup(final InitialContext initialContext, final String name, final Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(initialContext.lookup(name));
    }


    public static void dumpJndi(final InitialContext initialContext, final String s) {
        try {
            dumpTreeEntry(initialContext, initialContext.list(s), s);
        } catch (NamingException ignore) {
        }
    }

    private static void dumpTreeEntry(final InitialContext initialContext, final NamingEnumeration<NameClassPair> list, final String s) throws NamingException {
        System.out.println("\ndump " + s);
        while (list.hasMore()) {
            final NameClassPair ncp = list.next();
            System.out.println(ncp.toString());
            if (s.length() == 0) {
                dumpJndi(initialContext, ncp.getName());
            } else {
                dumpJndi(initialContext, s + "/" + ncp.getName());
            }
        }
    }
}
