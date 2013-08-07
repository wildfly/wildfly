package org.wildfly.extension.undertow;

/**
 *
 * Service that provides the default session cookie config. The config can be overriden on a per
 * app basis, and may not be present.
 *
 * @author Stuart Douglas
 */
public class SessionCookieConfig {

    private final String name;
    private final String domain;
    private final String comment;
    private final Boolean httpOnly;
    private final Boolean secure;
    private final Integer maxAge;

    public SessionCookieConfig(final String name, final String domain, final String comment, final Boolean httpOnly, final Boolean secure, final Integer maxAge) {
        this.name = name;
        this.domain = domain;
        this.comment = comment;
        this.httpOnly = httpOnly;
        this.secure = secure;
        this.maxAge = maxAge;
    }

    public String getName() {
        return name;
    }

    public String getDomain() {
        return domain;
    }

    public String getComment() {
        return comment;
    }

    public Boolean getHttpOnly() {
        return httpOnly;
    }

    public Boolean getSecure() {
        return secure;
    }

    public Integer getMaxAge() {
        return maxAge;
    }
}
