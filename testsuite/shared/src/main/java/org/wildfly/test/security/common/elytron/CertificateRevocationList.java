/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.security.common.elytron;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Helper class for adding "certificate-revocation-list" attribute.
 *
 * @author Jan Stourac <jstourac@redhat.com>
 */
public class CertificateRevocationList implements CliFragment {

    public static final CertificateRevocationList EMPTY = CertificateRevocationList.builder().build();

    private final String path;
    private final String relativeTo;

    private CertificateRevocationList(Builder builder) {
        this.path = builder.path;
        this.relativeTo = builder.relativeTo;
    }

    @Override
    public String asString() {
        StringBuilder sb = new StringBuilder();
        if (isNotBlank(path)) {
            sb.append(String.format("{path=\"%s\", ", path));
        }

        if (isNotBlank(relativeTo)) {
            sb.append(String.format("relative-to=\"%s\", ", relativeTo));
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Creates builder to build {@link CertificateRevocationList}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link CertificateRevocationList}.
     */
    public static final class Builder {
        private String path;
        private String relativeTo;

        private Builder() {
        }

        public Builder withPath(String path) {
            this.path = path;
            return this;
        }

        public Builder withRelativeTo(String relativeTo) {
            this.relativeTo = relativeTo;
            return this;
        }

        public CertificateRevocationList build() {
            return new CertificateRevocationList(this);
        }
    }

}
