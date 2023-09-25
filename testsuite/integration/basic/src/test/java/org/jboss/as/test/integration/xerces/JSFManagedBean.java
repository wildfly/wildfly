/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.xerces;

import java.io.Serializable;
import jakarta.faces.annotation.FacesConfig;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

/**
 * User: jpai
 */
@Named
@ViewScoped
@FacesConfig
public class JSFManagedBean implements Serializable {
    private static final long serialVersionUID = 1L;
}
