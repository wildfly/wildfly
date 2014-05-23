package org.jboss.as.test.manualmode.web.websocket;

import javax.enterprise.context.RequestScoped;

/**
 * @author Stuart Douglas
 */
@RequestScoped
public class CdiBean {

    public String getMessage() {
        return "Reply to ";
    }
}
