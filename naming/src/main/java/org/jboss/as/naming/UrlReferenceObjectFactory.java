/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming;

import java.net.URI;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

/**
 * @author pskopek
 *
 */
public class UrlReferenceObjectFactory implements ObjectFactory {

    /*
     * (non-Javadoc)
     *
     * @see javax.naming.spi.ObjectFactory#getObjectInstance(java.lang.Object, javax.naming.Name, javax.naming.Context,
     * java.util.Hashtable)
     */
    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        Reference reference = (Reference) obj;
        RefAddr ra = reference.get("URL");
        String referralUrl = (String) ra.getContent();
        String scheme = new URI(referralUrl).getScheme();
        // environment.remove(Context.URL_PKG_PREFIXES);
        return getURLObject(scheme, referralUrl, name, nameCtx, environment);
    }

    private Object getURLObject(String scheme, String referralUrl, Name name, Context nameCtx, Hashtable<?, ?> environment)
            throws NamingException {
        return com.sun.jndi.ldap.LdapCtxFactory.getLdapCtxInstance(referralUrl, environment);
    }
}
