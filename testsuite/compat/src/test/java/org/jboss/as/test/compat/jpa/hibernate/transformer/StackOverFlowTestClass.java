package org.jboss.as.test.compat.jpa.hibernate.transformer;

public class StackOverFlowTestClass {
    // commenting this method out avoids stackoverflow
    public static String test(String aKey) {
    // commenting out the try/catch avoids stackoverflow
        try {
            StackOverFlowTestClass vAnchorBundle = getNothing();
            if (vAnchorBundle != null)
                return aKey;
        } catch (Exception e3) {
        }
        return aKey;
    }

    // commenting out the call to this method also avoids stackoverflow
    public static StackOverFlowTestClass getNothing() {
        return null;
    }
}
