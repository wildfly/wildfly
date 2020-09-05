package org.wildfly.test.integration.microprofile.opentracing.jaxrs.application.model;

public class TestResponse {

    private final long randomId;
    private final String randomContent;

    public TestResponse(long randomId, String randomContent) {
      this.randomId = randomId;
      this.randomContent = randomContent;
    }

    public long getRandomId() {
      return randomId;
    }

    public String getRandomContent() {
      return randomContent;
    }

  }
