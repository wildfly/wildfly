/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer.prometheus;

import java.util.HashMap;
import java.util.Map;

record MediaType(String type, String subtype, Map<String, String> parameters) implements Comparable<MediaType> {

    static final String WILDCARD = "*";

    static MediaType parse(String input) {
        String[] parts = input.split(";");
        String[] types = parts[0].trim().split("/");

        String type = types[0].trim();
        type = type.isEmpty() ? WILDCARD : type;
        String subtype = types.length > 1 ? types[1].trim() : WILDCARD;

        Map<String, String> params = new HashMap<>();
        params.put("q", "1.0");
        for (int i = 1; i < parts.length; i++) {
            String param = parts[i].trim();
            if (param.isEmpty()) {
                continue;
            }

            String[] kv = param.split("=", 2);
            String key = kv[0].trim();
            String value = kv.length > 1 ? kv[1].trim().replace("\"", "") : "";

            if ("q".equalsIgnoreCase(key)) {
                try {
                    double q = Double.parseDouble(value);
                    // https://datatracker.ietf.org/doc/html/rfc9110#section-12.4.2
                    q = Math.max(0, Math.min(1, q));
                    params.put(key, Double.toString(q));
                } catch (NumberFormatException ignored) {
                }
            } else {
                params.put(key, value);
            }
        }

        return new MediaType(type, subtype, Map.copyOf(params));
    }

    boolean matches(String otherType, String otherSubtype) {
        return (type.equalsIgnoreCase(otherType) || WILDCARD.equals(type) || WILDCARD.equals(otherType)) &&
                (subtype.equalsIgnoreCase(otherSubtype) || WILDCARD.equals(subtype) || WILDCARD.equals(otherSubtype));
    }

    private int specificity() {
        if (WILDCARD.equals(type) && WILDCARD.equals(subtype)) {
            return 0;
        }
        if (WILDCARD.equals(subtype)) {
            return 1;
        }
        return 2;
    }

    @Override
    public int compareTo(MediaType o) {
        int qCompare = Double.compare(Double.parseDouble(o.parameters.getOrDefault("q", "1.0")),
                Double.parseDouble(this.parameters.getOrDefault("q", "1.0")));
        if (qCompare != 0) {
            return qCompare;
        }
        return Integer.compare(o.specificity(), this.specificity());
    }

    @Override
    public String toString() {
        return asHeaderString() + ";q=" + parameters.get("q");
    }

    String asHeaderString() {
        return type + "/" + subtype + parametersStringWithoutQ();
    }

    private String parametersStringWithoutQ() {
        if (parameters.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && !"q".equalsIgnoreCase(entry.getKey())) {
                sb.append(";").append(entry.getKey()).append("=").append(entry.getValue());
            }
        }
        return sb.toString();
    }

}