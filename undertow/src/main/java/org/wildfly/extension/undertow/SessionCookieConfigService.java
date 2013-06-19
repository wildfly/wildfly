package org.wildfly.extension.undertow;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 *
 * Service that provides the default session cookie config. The config can be overriden on a per
 * app basis, and may not be present.
 *
 * @author Stuart Douglas
 */
public class SessionCookieConfigService implements Service<SessionCookieConfigService> {

    public static final ServiceName SERVICE_NAME = UndertowService.SERVLET_CONTAINER.append("session-cookie-config");

    private final String name;
    private final String domain;
    private final String comment;
    private final Boolean httpOnly;
    private final Boolean secure;
    private final Integer maxAge;

    public SessionCookieConfigService(final String name, final String domain, final String comment, final Boolean httpOnly, final Boolean secure, final Integer maxAge) {
        this.name = name;
        this.domain = domain;
        this.comment = comment;
        this.httpOnly = httpOnly;
        this.secure = secure;
        this.maxAge = maxAge;
    }

    @Override
    public void start(final StartContext context) throws StartException {

    }

    @Override
    public void stop(final StopContext context) {

    }

    @Override
    public SessionCookieConfigService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
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
