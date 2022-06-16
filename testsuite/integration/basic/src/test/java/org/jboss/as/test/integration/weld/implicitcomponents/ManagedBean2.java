package org.jboss.as.test.integration.weld.implicitcomponents;

import jakarta.annotation.ManagedBean;

/**
 * @author Stuart Douglas
 */
@ManagedBean
public class ManagedBean2 {

    public static final String MESSAGE = "Hello from bean 2";

    public String getMessage() {
        return MESSAGE;
    }

}
