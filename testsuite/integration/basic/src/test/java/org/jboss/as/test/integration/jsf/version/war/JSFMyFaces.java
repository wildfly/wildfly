/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jsf.version.war;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.annotation.FacesConfig;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.Serializable;

@Named("jsfmyfacesversion")
@SessionScoped
@FacesConfig
public class JSFMyFaces implements Serializable {

    /** Default value included to remove warning. **/
    private static final long serialVersionUID = 1L;

    public String getJSFVersion() {
        return "MyFaces Bundled:" + FacesContext.class.getPackage().getSpecificationTitle();
    }
}
