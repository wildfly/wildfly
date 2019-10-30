package org.jboss.as.test.integration.deployment.subdirectory;

import javax.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless
public class MessageBean {

    public String getMessage() {
        return "Hello World";
    }
}
