/*
 * Copyright 2019 Red Hat, Inc.
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
package org.wildfly.test.integration.weld.builtinBeans;

import javax.annotation.security.RunAs;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.jboss.ejb3.annotation.RunAsPrincipal;

@Stateless
@RunAs("Admin")
@RunAsPrincipal("non-anonymous")
public class CallerWithIdentity {

    @Inject
    BeanWithInjectedPrincipal beanA;

    @Inject
    BeanWithPrincipalFromEJBContext beanB;

    @Inject
    BeanWithSecuredPrincipal beanC;

    public String getCallerPrincipalInjected() {
        return beanA.getPrincipalName();
    }

    public String getCallerPrincipalFromEJBContext() {
        return beanB.getPrincipalName();
    }

    public String getCallerPrincipalFromEJBContextSecuredBean() {
        return beanC.getPrincipalName();
    }
}
