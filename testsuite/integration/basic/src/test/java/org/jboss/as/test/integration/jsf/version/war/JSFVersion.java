/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jsf.version.war;

import org.jboss.as.test.integration.jsf.version.ejb.JSFVersionEJB;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;

import java.io.Serializable;

@Named("jsfversion")
@SessionScoped
public class JSFVersion implements Serializable {

    /** Default value included to remove warning. **/
    private static final long serialVersionUID = 1L;

    /**
     * Injected JSFVersionEJB client
     */
    @EJB
    private JSFVersionEJB jsfVersionEJB;

    public String getJSFVersion() {
        return jsfVersionEJB.getJSFVersion();
    }
}
