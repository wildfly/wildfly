package org.jboss.as.test.integration.undertow;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@XmlRootElement
public class Response {

    public static Response getResponse() throws IOException {
        Response r = new Response();
        r.setCode(200);
        r.setDescription("Retrieval sucessful");
        InputStream is = Response.class.getResource("/content").openStream();
        String content = "";
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(is))) {
            content = buffer.lines().collect(Collectors.joining());
        }
        r.setDocument(content);
        return r;
    }

    private int code;

    private String description;

    private String document;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

}
