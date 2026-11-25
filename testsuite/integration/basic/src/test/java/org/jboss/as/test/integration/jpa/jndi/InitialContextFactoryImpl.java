package org.jboss.as.test.integration.jpa.jndi;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

/**
 * InitialContextFactoryImpl
 *
 * @author Scott Marlow
 */
public class InitialContextFactoryImpl implements InitialContextFactory {
    public static boolean wasCalled = false;
    public Context getInitialContext(Hashtable<?,?> environment) throws NamingException {
        Context initialContext = new InitialContext() {};
        wasCalled = true;
        return initialContext;
    }

    public static boolean wasCalled() {
        return wasCalled;
    }
}
