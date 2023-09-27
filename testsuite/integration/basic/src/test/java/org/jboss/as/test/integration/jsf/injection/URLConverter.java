/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jsf.injection;

import java.net.URI;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

/**
 * Converter that converts an object into an URL.
 *
 * @author rmartinc
 */
@FacesConverter(value = "urlConverter", managed = true)
public class URLConverter implements Converter {

    public URLConverter() {
    }

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        try {
            URI uri = new URI(value).parseServerAuthority();
            return uri.toURL();
        } catch (Exception e) {
            context.addMessage(component.getClientId(context), new FacesMessage("Invalid URL:", e.getMessage()));
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        return value.toString();
    }
}
