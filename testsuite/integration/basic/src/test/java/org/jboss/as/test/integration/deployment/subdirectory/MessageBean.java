package org.jboss.as.test.integration.deployment.subdirectory;

import jakarta.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless
public class MessageBean {

    public String getMessage() {
        return "Hello World";
    }
}
