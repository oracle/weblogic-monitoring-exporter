package com.oracle.wls.exporter;

import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;

public class HelidonInvocationContextFactory implements InvocationContextFactory {

    public static InvocationContextFactory create() {
        return new HelidonInvocationContextFactory();
    }

    @Override
    public InvocationContext createContext(ServerRequest req, ServerResponse res) {
        return new HelidonInvocationContext(req, res);
    }
}
