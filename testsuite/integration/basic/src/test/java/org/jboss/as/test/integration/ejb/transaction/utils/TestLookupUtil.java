package org.jboss.as.test.integration.ejb.transaction.utils;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.logging.Logger;

public final class TestLookupUtil {
    private static final Logger log = Logger.getLogger(TestLookupUtil.class);

    private TestLookupUtil() {
        // not instance here
    }

    public static <T> T lookupModule(InitialContext initCtx, Class<T> beanType) throws NamingException {
        String lookupString = String.format("java:module/%s!%s", beanType.getSimpleName(), beanType.getName());
        log.debug("looking for: " + lookupString);
        return beanType.cast(initCtx.lookup(lookupString));
    }
}
