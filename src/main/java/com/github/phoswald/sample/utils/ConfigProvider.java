package com.github.phoswald.sample.utils;

import java.util.Optional;

public class ConfigProvider {

	public Optional<String> getConfigProperty(String name) {
		String value = System.getProperty(name);
		if (value == null) {
			value = System.getenv().get(name.replace('.', '_').toUpperCase());
		}
		if (value != null) {
			return Optional.of(value);
		} else {
			return Optional.empty();
		}
	}
}
