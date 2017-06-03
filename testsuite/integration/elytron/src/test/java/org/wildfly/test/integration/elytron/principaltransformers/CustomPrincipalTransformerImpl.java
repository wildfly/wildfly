/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.integration.elytron.principaltransformers;

import java.security.Principal;
import java.util.Map;

import org.wildfly.extension.elytron.Configurable;
import org.wildfly.extension.elytron.capabilities.PrincipalTransformer;
import org.wildfly.security.auth.principal.NamePrincipal;

public class CustomPrincipalTransformerImpl implements PrincipalTransformer, Configurable {

    private String userName;

    @Override
    public Principal apply(Principal p) {
        return new NamePrincipal(userName);
    }

    @Override
    public void initialize(Map<String, String> configuration) {
        if (configuration.containsKey("throwException")) {
            throw new IllegalStateException("Only test purpose. This exception was thrown on demand.");
        }

        userName = configuration.getOrDefault("transformTo", null);
    }

}
