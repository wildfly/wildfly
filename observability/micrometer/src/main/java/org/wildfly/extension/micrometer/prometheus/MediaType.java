/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer.prometheus;

record MediaType(String type, String subtype, double q) implements Comparable<MediaType> {

    static final String WILDCARD = "*";

    static MediaType parse(String input) {
        String[] parts = input.split(";");
        String[] types = parts[0].trim().split("/");

        String type = types[0].trim();
        type = type.isEmpty() ? WILDCARD : type;
        String subtype = types.length > 1 ? types[1].trim() : WILDCARD;

        double q = 1.0;
        for (String p : parts) {
            p = p.trim();
            if (p.startsWith("q=")) {
                try {
                    q = Double.parseDouble(p.substring(2));
                    // https://datatracker.ietf.org/doc/html/rfc9110#section-12.4.2
                    q = Math.max(0, Math.min(1, q));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return new MediaType(type, subtype, q);
    }

    boolean matches(String otherType, String otherSubtype) {
        return (type.equalsIgnoreCase(otherType) || WILDCARD.equals(type) || WILDCARD.equals(otherType)) && (
                subtype.equalsIgnoreCase(otherSubtype) || WILDCARD.equals(subtype) || WILDCARD.equals(otherSubtype));
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
        int qCompare = Double.compare(o.q, this.q);
        if (qCompare != 0) {
            return qCompare;
        }

        return Integer.compare(o.specificity(), this.specificity());
    }

    @Override
    public String toString() {
        return asHeaderString() + ";q=" + q;
    }

    public String asHeaderString() {
        return type + "/" + subtype;
    }

}