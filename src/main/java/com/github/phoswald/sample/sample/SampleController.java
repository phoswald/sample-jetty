package com.github.phoswald.sample.sample;

import com.github.phoswald.sample.utils.ConfigProvider;

public class SampleController {

    private final String sampleConfig;

    public SampleController(ConfigProvider config) {
        this.sampleConfig = config.getConfigProperty("app.sample.config").orElse("Undefined");
    }

    public String getSamplePage() {
        return new SampleView().render(new SampleViewModel(sampleConfig));
    }
}
