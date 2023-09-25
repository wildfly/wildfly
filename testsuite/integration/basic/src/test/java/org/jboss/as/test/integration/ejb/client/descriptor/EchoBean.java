/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.client.descriptor;

import java.util.Hashtable;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;
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
