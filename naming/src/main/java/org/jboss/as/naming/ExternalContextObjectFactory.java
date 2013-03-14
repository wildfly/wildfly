package org.jboss.as.naming;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;

/**
 * object factory that creates a new initial context with the supplied properties
 *
 * @author Stuart Douglas
 */
public class ExternalContextObjectFactory implements ObjectFactory {
    @Override
    public Object getObjectInstance(final Object obj, final Name name, final Context nameCtx, final Hashtable<?, ?> environment) throws Exception {
        return new InitialContext((Hashtable)environment);
    }
}
