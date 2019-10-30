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

package org.jboss.as.test.integration.ejb.client.descriptor;

import java.util.Hashtable;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.naming.Context;

/**
 * @author Jaikiran Pai
 */
@Stateless
@Remote(RemoteEcho.class)
public class EchoBean implements RemoteEcho {

    @Override
    public String echo(final String moduleName, final String msg) {
        try {
            final Hashtable props = new Hashtable();
            props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
            final Context context = new javax.naming.InitialContext(props);
            RemoteEcho delegate = (RemoteEcho) context.lookup("ejb:" + "" + "/" + moduleName + "/" + "" + "/" + DelegateEchoBean.class.getSimpleName() + "!" + RemoteEcho.class.getName());
            return delegate.echo(moduleName, msg);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String twoSecondEcho(String moduleName, String msg) {
        final RemoteEcho delegate;
        try {
            final Hashtable props = new Hashtable();
            props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
            final Context context = new javax.naming.InitialContext(props);
            delegate = (RemoteEcho) context.lookup("ejb:" + "" + "/" + moduleName + "/" + "" + "/" + DelegateEchoBean.class.getSimpleName() + "!" + RemoteEcho.class.getName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return delegate.twoSecondEcho(moduleName, msg);
    }
}
