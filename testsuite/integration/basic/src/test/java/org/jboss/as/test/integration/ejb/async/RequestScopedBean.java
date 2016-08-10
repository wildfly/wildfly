package org.jboss.as.test.integration.ejb.async;

import javax.enterprise.context.RequestScoped;

/**
 * @author Stuart Douglas
 */
@RequestScoped
public class RequestScopedBean {
    private int state = 0;

    public int getState() {
        return state;
    }

    public void setState(final int state) {
        this.state = state;
    }
}
