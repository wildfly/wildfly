/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.beanvalidation;

/**
 * A simple bean with no annotation-based validation constraints.
 * Constraints are configured via validation.xml / constraint-mapping.xml
 * to verify that validation.xml is properly located in WAR-in-EAR deployments.
 */
public class ValidationXmlEarBean {

    private String name;
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
