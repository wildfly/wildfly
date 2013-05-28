package org.jboss.as.domain.http.server;

import io.undertow.util.ETag;

/**
 * Value class for describing the result of an operation against the {@link DomainApiHandler}.
 * Used by {@link DomainUtil#writeResponse(io.undertow.server.HttpServerExchange, int, org.jboss.dmr.ModelNode, OperationParameter)}
 *
 * @author Harald Pehl
 * @date 05/14/2013
 */
public class OperationParameter {
    private final boolean get;
    private final int maxAge;
    private final ETag etag;
    private final boolean encode;
    private final boolean pretty;

    private OperationParameter(Builder builder) {
        this.get = builder.get;
        this.maxAge = builder.maxAge;
        this.etag = builder.etag;
        this.encode = builder.encode;
        this.pretty = builder.pretty;
    }

    public boolean isGet() {
        return get;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public ETag getEtag() {
        return etag;
    }

    public boolean isEncode() {
        return encode;
    }

    public boolean isPretty() {
        return pretty;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("OperationResult{");
        sb.append("get=").append(get);
        sb.append(", maxAge=").append(maxAge);
        sb.append(", etag=").append(etag);
        sb.append(", encode=").append(encode);
        sb.append(", pretty=").append(pretty);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Builder for {@link OperationParameter}.
     */
    public static class Builder {
        // mandatory
        private final boolean get;
        // optional
        private int maxAge;
        private ETag etag;
        private boolean pretty;
        private boolean encode;

        /**
         * Creates a new builder.
         * <p>Mandatory parameter</p>
         * <ol>
         *     <li>get</li>
         * </ol>
         * <p>Optional parameter (and their default values)</p>
         * <ul>
         *     <li>maxAge (0)</li>
         *     <li>etag (null)</li>
         *     <li>encode (false)</li>
         *     <li>pretty (false)</li>
         * </ul>
         *
         * @param get
         */
        public Builder(final boolean get) {
            this.get = get;
            this.maxAge = 0;
            this.encode = false;
            this.pretty = false;
        }

        public Builder maxAge(int maxAge) {
            this.maxAge = maxAge;
            return this;
        }

        public Builder etag(ETag etag) {
            this.etag = etag;
            return this;
        }

        public Builder encode(boolean encode) {
            this.encode = encode;
            return this;
        }

        public Builder pretty(boolean pretty) {
            this.pretty = pretty;
            return this;
        }

        public OperationParameter build() {
            return new OperationParameter(this);
        }
    }
}
