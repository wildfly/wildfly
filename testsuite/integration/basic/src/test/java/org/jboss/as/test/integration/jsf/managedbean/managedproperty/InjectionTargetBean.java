/*
 * JBoss, Home of Professional Open Source
 * Copyright 2022, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
