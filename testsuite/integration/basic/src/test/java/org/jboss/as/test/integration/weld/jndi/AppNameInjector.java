package org.jboss.as.test.integration.weld.jndi;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;

/**
 * @author Stuart Douglas
 */
@ApplicationScoped
public class AppNameInjector {

    @Resource(name = "java:global/foo")
    private String name;

    public String getFoo() {
        return name;
    }

}
