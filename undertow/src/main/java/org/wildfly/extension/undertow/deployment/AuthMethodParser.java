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
                    Map<String, Deque<String>> props = QueryParameterUtils.parseQueryString(part.substring(index + 1));
                    final AuthMethodConfig authMethodConfig = createAuthMethodConfig(name, replacements);
                    for (Map.Entry<String, Deque<String>> entry : props.entrySet()) {
                        Deque<String> val = entry.getValue();
                        if (val.isEmpty()) {
                            authMethodConfig.getProperties().put(URLDecoder.decode(entry.getKey(), UTF_8), "");
                        } else {
                            authMethodConfig.getProperties().put(URLDecoder.decode(entry.getKey(), UTF_8), URLDecoder.decode(val.getFirst(), UTF_8));
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
