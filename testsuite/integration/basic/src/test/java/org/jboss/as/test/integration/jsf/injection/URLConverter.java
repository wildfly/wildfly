/*
 * Copyright (c) 2020. Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.jsf.injection;

import java.net.URI;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

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
