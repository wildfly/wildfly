/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.iiop.openjdk.naming.jndi;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;

/**
 * Adaptor that allows cosnaming to work inside the AS.
 *
 * @author Stuart Douglas
 */
public class JBossCNCtxFactory implements ObjectFactory {

    public static final JBossCNCtxFactory INSTANCE = new JBossCNCtxFactory();

    private JBossCNCtxFactory() {

    }

    @Override
    public Object getObjectInstance(final Object obj, final Name name, final Context nameCtx, final Hashtable environment) throws Exception {
        return new WrapperInitialContext(environment);
    }

}
