package org.jboss.as.test.integration.ws.wsse.trust.stsholderofkey;

import org.jboss.wsf.stack.cxf.extensions.security.PasswordCallbackHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * User: rsearls
 * Date: 3/19/14
 */
public class STSHolderOfKeyCallbackHandler extends PasswordCallbackHandler {
    public STSHolderOfKeyCallbackHandler() {
        super(getInitMap());
    }

    private static Map<String, String> getInitMap() {
        Map<String, String> passwords = new HashMap<String, String>();
        passwords.put("mystskey", "stskpass");
        passwords.put("alice", "clarinet");
        return passwords;
    }
}
