package org.jboss.as.test.integration.ejb.interceptor.annotatedejbclient;

import jakarta.ejb.Remote;
import org.jboss.ejb.client.annotation.ClientInterceptors;

@ClientInterceptors({ClientInterceptor.class})
@Remote
public interface TestRemote {
    int invoke(String id);
}
