/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jsf.managedbean.managedproperty;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.annotation.ManagedProperty;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * A CDI bean that injects various beans provided by Jakarta Faces
 * via its @ManagedProperty facility.
 *
 * @author Farah Juma
 * @author Brian Stansberry
 */
@Named("testTarget")
@RequestScoped
public class InjectionTargetBean {

    private boolean postConstructCalled = false;
    private boolean greetingBeanInjected = false;

    /** Injects using the Faces facility that exposes the request parameter map */
    @Inject
    @ManagedProperty(value = "#{param.testName}")
    private String testName;

    /** Injects using the Faces facility that exposes the FacesContext */
    @Inject
    @ManagedProperty("#{facesContext.externalContext.requestContextPath}")
    private String contextPath;

    /** Injects a bean included in the deployment */
    @Inject
    @ManagedProperty(value = "#{greetingBean}")
    private GreetingBean greetingBean;

    @PostConstruct
    public void postConstruct() {
        if ((greetingBean != null) && greetingBean.greet("Bob").equals("Hello Bob")) {
            greetingBeanInjected = true;
        }

        postConstructCalled = true;
    }

    public String getTestName() {
        return testName;
    }

    public String getContextPath() {
        return contextPath;
    }

    public boolean isGreetingBeanInjected() {
        return greetingBeanInjected;
    }

    public boolean isPostConstructCalled() {
        return postConstructCalled;
    }
}
