/*
 * Copyright 2016 Red Hat, Inc.
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
package org.wildfly.test.manual.elytron.seccontext;

import java.net.URL;
import javax.ejb.Remote;

/**
 * Interface for the bean used as the entry point to verify EJB3 security behaviour.
 */
@Remote
public interface Entry {

    /**
     * @return The name of the Principal obtained from a call to EJBContext.getCallerPrincipal()
     */
    String whoAmI();

    /**
     * Obtains the name of the Principal obtained from a call to EJBContext.getCallerPrincipal() both for the bean called and
     * also from a call to a second bean (user may be switched before the second call - depending on arguments).
     *
     * @return An array containing the name from the local call first followed by the name from the second call.
     * @throws Exception - If there is an unexpected failure establishing the security context for the second call.
     */
    String[] doubleWhoAmI(CallAnotherBeanInfo info);

    /**
     * Obtains the name of the Principal obtained from a call to EJBContext.getCallerPrincipal() for the bean called and
     * obtains IllegalStateException from a call to a second bean.
     *
     * @return An array containing the name from the local call first followed by the IllegalStateException from the second call.
     * @throws Exception - If there is an unexpected failure establishing the security context for the second call.
     */
    String[] whoAmIAndIllegalStateException(CallAnotherBeanInfo info);

    /**
     * Obtains the name of the Principal obtained from a call to EJBContext.getCallerPrincipal() for the bean called and
     * obtains Server2Exception from a call to a second bean.
     *
     * @return An array containing the name from the local call first followed by the Server2Exception from the second call.
     * @throws Exception - If there is an unexpected failure establishing the security context for the second call.
     */
    String[] whoAmIAndServer2Exception(CallAnotherBeanInfo info);

    /**
     * Read remote URL using simple HttpURLConnection.
     */
    String readUrl(String username, String password, ReAuthnType type, final URL url);
}
