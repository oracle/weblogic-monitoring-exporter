package com.oracle.wls.exporter;

import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;

public interface InvocationContextFactory {
    InvocationContext createContext(ServerRequest req, ServerResponse res);
}
