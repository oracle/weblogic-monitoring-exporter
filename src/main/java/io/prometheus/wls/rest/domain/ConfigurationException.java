package io.prometheus.wls.rest.domain;

public class ConfigurationException extends RuntimeException {
    public ConfigurationException(String description) {
        super(description);
    }
}
