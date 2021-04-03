package com.github.phoswald.sample.sample;

import com.github.phoswald.sample.ConfigProvider;

public class SampleController {

    private final String sampleConfig = new ConfigProvider().getConfigProperty("app.sample.config").orElse("Undefined");

    public String getSamplePage() {
        return new SampleView().render(new SampleViewModel(sampleConfig));
    }
}
