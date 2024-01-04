/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.wsse.trust.actas;

import org.jboss.wsf.stack.cxf.extensions.security.PasswordCallbackHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * User: rsearls@redhat.com
 * Date: 1/26/14
 */
public class ActAsCallbackHandler extends PasswordCallbackHandler {

    public ActAsCallbackHandler() {
        super(getInitMap());
    }

    private static Map<String, String> getInitMap() {
        Map<String, String> passwords = new HashMap<String, String>();
        passwords.put("myactaskey", "aspass");
        passwords.put("alice", "clarinet");
        return passwords;
    }

}
