package org.jboss.as.test.integration.jsf.jta;

import javax.naming.InitialContext;
import javax.transaction.UserTransaction;

import junit.framework.Assert;

/**
 * Base class for jsf.jta tests.
 *
 * @author baranowb
 */
public class JTATestsBase {

    public static final String NAME = "java:comp/UserTransaction";

    public static void doLookupTest() {
        // FIXME: this will actaully make fail whole app to load, causing 500 response to initial get.
        try {
            InitialContext ic = new InitialContext();
            Object o = ic.lookup(NAME);
            Assert.assertNotNull(o);
            Assert.assertTrue(o instanceof UserTransaction);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
