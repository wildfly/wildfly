package org.jboss.as.webservices.deployers;

/**
 * User: rsearls
 * Date: 7/17/14
 */
public class WebContextAnnotationInfo {
    private final String authMethod;
    private final String contextRoot;
    private final boolean secureWSDLAccess;
    private final String transportGuarantee;
    private final String urlPattern;
    private final String virtualHost;

    public WebContextAnnotationInfo(final String authMethod, final String contextRoot, final boolean secureWSDLAccess, final String transportGuarantee, final String urlPattern, final String virtualHost) {

        this.authMethod = authMethod;
        this.contextRoot = contextRoot;
        this.secureWSDLAccess = secureWSDLAccess;
        this.transportGuarantee = transportGuarantee;
        this.urlPattern = urlPattern;
        this.virtualHost = virtualHost;

    }

    public String getAuthMethod() {
        return authMethod;
    }

    public String getContextRoot() {
        return contextRoot;
    }

    public boolean isSecureWSDLAccess() {
        return secureWSDLAccess;
    }

    public String getTransportGuarantee() {
        return transportGuarantee;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public String getVirtualHost() {
        return virtualHost;
    }

}
