package io.prometheus.wls.rest.domain;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationException extends RuntimeException {
    private List<String> context = new ArrayList<>();

    ConfigurationException(String description) {
        super(description);
    }

    void addContext(String parentContext) {
        context.add(0, parentContext);
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        if (!context.isEmpty())
            sb.append(" at ").append(String.join(".", context));
        return sb.toString();
    }
}
