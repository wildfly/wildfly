/*
 * Copyright 2023 Red Hat, Inc.
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
package org.wildfly.test.integration.elytron.securityapi;

import java.security.Principal;
import java.time.LocalDateTime;

import jakarta.security.enterprise.CallerPrincipal;

/**
 * A simple {@link jakarta.security.enterprise.CallerPrincipal} with a custom field and method.
 *
 * @author <a href="mailto:carodrig@redhat.com">Cameron Rodriguez</a>
 */
public class TestCustomPrincipal extends CallerPrincipal {

    private static final long serialVersionUID = -35690086418605259L;
    private final LocalDateTime currentLoginTime;
    private final Principal wrappedPrincipal;

    public TestCustomPrincipal(Principal principal) {
        super(principal.getName());
        this.wrappedPrincipal = principal;
        this.currentLoginTime = LocalDateTime.now();
    }

    public LocalDateTime getCurrentLoginTime() {
        return this.currentLoginTime;
    }

    @Override
    public String getName() {
        return wrappedPrincipal.getName();
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TestCustomPrincipal) {
            return this.getName().equals(((TestCustomPrincipal) obj).getName());
        } else {
            return super.equals(obj);
        }
    }
}
