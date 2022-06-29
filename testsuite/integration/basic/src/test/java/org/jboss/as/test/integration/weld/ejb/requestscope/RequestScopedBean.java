package org.jboss.as.test.integration.weld.ejb.requestscope;

import jakarta.enterprise.context.RequestScoped;

/**
 * @author Stuart Douglas
 */
@RequestScoped
public class RequestScopedBean {

    public static final String MESSAGE = "Request Scoped";

    public String getMessage() {
        return MESSAGE;
    }
}
