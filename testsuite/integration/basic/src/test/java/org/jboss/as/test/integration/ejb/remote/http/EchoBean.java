package org.jboss.as.test.integration.ejb.remote.http;

import javax.ejb.Remote;
import javax.ejb.Stateless;

@Stateless
@Remote(EchoRemote.class)
public class EchoBean implements EchoRemote {
    @Override
    public String echo(String arg) {
        return arg;
    }
}
