package org.jboss.as.test.integration.ejb.remote.requestdeserialization;

import java.io.Serializable;

public class Response implements Serializable {

    private String greeting;
    private String tccl;

    public Response() {
    }

    public Response(String greeting, String tccl) {
        this.greeting = greeting;
        this.tccl = tccl;
    }

    public String getGreeting() {
        return greeting;
    }

    public String getTccl() {
        return tccl;
    }

}
