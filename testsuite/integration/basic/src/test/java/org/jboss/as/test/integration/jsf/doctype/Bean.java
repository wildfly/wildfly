/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jsf.doctype;

import java.io.Serializable;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.annotation.FacesConfig;
import jakarta.inject.Named;

/**
 * A simple bean.
 *
 * @author <a href="fjuma@redhat.com">Farah Juma</a>
 */
@Named("bean")
@SessionScoped
@FacesConfig
public class Bean implements Serializable {

    private String name;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
