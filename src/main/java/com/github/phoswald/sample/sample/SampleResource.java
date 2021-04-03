package com.github.phoswald.sample.sample;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.github.phoswald.sample.ConfigProvider;

public class SampleResource {

    private final String sampleConfig = new ConfigProvider().getConfigProperty("app.sample.config").orElse("Undefined");

    public String getTime() {
        return ZonedDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
    }

    public String getConfig() {
        return sampleConfig;
    }

    public EchoResponse postEcho(EchoRequest request) {
        EchoResponse response = new EchoResponse();
        response.setOuput("Received " + request.getInput());
        return response;
    }
}
