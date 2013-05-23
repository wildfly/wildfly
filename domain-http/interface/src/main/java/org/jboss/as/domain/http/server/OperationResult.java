package org.jboss.as.domain.http.server;

import java.util.Date;

import org.jboss.dmr.ModelNode;

/**
 * Value class for describing the result of an operation against the {@link DomainApiHandler}.
 * Used by {@link DomainUtil#writeResponse(io.undertow.server.HttpServerExchange, OperationResult)}
 *
 * @author Harald Pehl
 * @date 05/14/2013
 */
public class OperationResult {
    private final ModelNode response;
    private final int status;
    private final int maxAge;
    private final Date lastModified;
    private final boolean encode;
    private final boolean pretty;

    private OperationResult(Builder builder) {
        this.response = builder.response;
        this.status = builder.status;
        this.maxAge = builder.maxAge;
        this.lastModified = builder.lastModified;
        this.encode = builder.encode;
        this.pretty = builder.pretty;
    }

    public ModelNode getResponse() {
        return response;
    }

    public int getStatus() {
        return status;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public boolean isEncode() {
        return encode;
    }

    public boolean isPretty() {
        return pretty;
    }

    /**
     * Builder for {@link OperationResult}.
     */
    public static class Builder {
        private final int status;
        // mandatory
        private ModelNode response;
        // optional
        private int maxAge;
        private Date lastModified;
        private boolean pretty;
        private boolean encode;

        /**
         * Creates a new builder.
         * <p>Mandatory parameter</p>
         * <ol>
         *     <li>response</li>
         *     <li>status</li>
         * </ol>
         * <p>Optional parameter (and their default values)</p>
         * <ul>
         *     <li>maxAge (0)</li>
         *     <li>lastModified (null)</li>
         *     <li>encode (false)</li>
         *     <li>pretty (false)</li>
         * </ul>
         *
         * @param response
         * @param status
         */
        public Builder(final ModelNode response, final int status) {
            this.response = response;
            this.status = status;
            this.maxAge = 0;
            this.lastModified = null;
            this.encode = false;
            this.pretty = false;
        }

        public Builder maxAge(int maxAge) {
            this.maxAge = maxAge;
            return this;
        }

        public Builder lastModified(Date lastModified) {
            this.lastModified = lastModified;
            return this;
        }

        public Builder response(ModelNode response) {
            this.response = response;
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

        public OperationResult build() {
            return new OperationResult(this);
        }
    }
}
