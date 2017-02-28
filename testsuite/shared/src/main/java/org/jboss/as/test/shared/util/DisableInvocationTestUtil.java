package org.jboss.as.test.shared.util;

/**
 * @author Kabir Khan
 */
public class DisableInvocationTestUtil {
    public static void disable() {
        AssumeTestGroupUtil.assumeInvocationTestsEnabled();
    }
}
