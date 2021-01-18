/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.jsf.resourceResolver;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import javax.faces.FacesException;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.FaceletsResourceResolver;
import javax.faces.view.facelets.ResourceResolver;

/**
 * <p>Custom Resource Resolver that just uses the ExternalContext to locate
 * the files inside the application. It also adds a message in the application
 * map to register it was used.</p>
 *
 * @author rmartinc
 */
@FaceletsResourceResolver
public class CustomResourceResolver extends ResourceResolver {

    public CustomResourceResolver() {
    }

    @Override
    public URL resolveUrl(String path) {
       final ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        if (!"/".equals(path)) {
            // add the message inside the application map
            Map<String, Object> appMap = externalContext.getApplicationMap();
            appMap.put("message", this.getClass().getName());
        }
        try {
            // locate the path using the external/servlet context
            return externalContext.getResource(path);
        } catch (MalformedURLException e) {
            throw new FacesException(e);
        }
    }
}