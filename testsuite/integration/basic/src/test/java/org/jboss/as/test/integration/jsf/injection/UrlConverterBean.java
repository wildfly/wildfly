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

import java.net.URL;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;

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
