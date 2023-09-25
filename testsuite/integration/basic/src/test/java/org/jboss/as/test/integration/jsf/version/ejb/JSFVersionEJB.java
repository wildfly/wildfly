/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jsf.version.ejb;

import jakarta.ejb.Stateless;
import jakarta.faces.context.FacesContext;

@Stateless
public class JSFVersionEJB {

    public String getJSFVersion() {
        return "JSF VERSION: " + FacesContext.class.getPackage().getSpecificationTitle();
    }
}
