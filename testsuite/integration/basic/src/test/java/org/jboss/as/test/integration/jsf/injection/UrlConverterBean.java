/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jsf.injection;

import java.net.URL;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Converter bean used in the url.xhtml.
 *
 * @author rmartinc
 */
@Named(value="urlConverterBean")
@RequestScoped
public class UrlConverterBean {

    @Inject
    @FacesConverter(value = "urlConverter", managed = true)
    private URLConverter urlConverter;

    @Inject
    private FacesContext context;

    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String submit(UIComponent component) {
        URL url = (URL) urlConverter.getAsObject(context, component, value);
        if (url != null) {
            // do whatever with the URL
            context.addMessage(component.getClientId(context), new FacesMessage("Valid URL.", ""));
        }
        return "";
    }
}
