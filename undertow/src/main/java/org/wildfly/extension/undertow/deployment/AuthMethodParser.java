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
package org.wildfly.extension.undertow.deployment;

import io.undertow.servlet.api.AuthMethodConfig;
import io.undertow.util.QueryParameterUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * @author Stuart Douglas
 */
public class AuthMethodParser {

    public static final String UTF_8 = "UTF-8";

    public static List<AuthMethodConfig> parse(final String methods, final Map<String, String> replacements) {
        try {
            if (methods == null || methods.isEmpty()) {
                return Collections.emptyList();
            }
            final List<AuthMethodConfig> ret = new ArrayList<AuthMethodConfig>();
            String[] parts = methods.split(",");
            for (String part : parts) {
                if (part.isEmpty()) {
                    continue;
                }
                int index = part.indexOf('?');
                if (index == -1) {
                    ret.add(createAuthMethodConfig(part, replacements));
                } else {
                    final String name = part.substring(0, index);
                    Map<String, Deque<String>> props = QueryParameterUtils.parseQueryString(part.substring(index + 1), UTF_8);
                    final AuthMethodConfig authMethodConfig = createAuthMethodConfig(name, replacements);
                    for (Map.Entry<String, Deque<String>> entry : props.entrySet()) {
                        Deque<String> val = entry.getValue();
                        if (val.isEmpty()) {
                            authMethodConfig.getProperties().put(entry.getKey(), "");
                        } else {
                            authMethodConfig.getProperties().put(entry.getKey(), val.getFirst());
                        }
                    }
                    ret.add(authMethodConfig);
                }
            }
            return ret;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static AuthMethodConfig createAuthMethodConfig(String part, Map<String, String> replacements) throws UnsupportedEncodingException {
        String name = URLDecoder.decode(part, UTF_8);
        if (replacements.containsKey(name)) {
            return new AuthMethodConfig(replacements.get(name));
        }
        return new AuthMethodConfig(name);
    }

    private AuthMethodParser() {

    }

}
