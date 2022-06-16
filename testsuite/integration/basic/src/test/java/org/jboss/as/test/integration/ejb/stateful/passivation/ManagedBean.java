package org.jboss.as.test.integration.ejb.stateful.passivation;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * @author Marek Schmidt
 */
@ApplicationScoped
public class ManagedBean {
    private String message = "foo";

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
