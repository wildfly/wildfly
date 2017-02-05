package org.jboss.as.test.shared.util;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.junit.Assume;

/**
 * @author Kabir Khan
 */
public class DisableInvocationTestUtil {
    public static void disable() {
        final boolean enableInvocationTests = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return Boolean.getBoolean("wildfly.tmp.enable.invocation.tests");
            }
        });
        Assume.assumeTrue(enableInvocationTests);
    }
}
