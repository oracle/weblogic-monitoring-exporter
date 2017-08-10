package io.prometheus.wls.rest;

public interface StatusCodes {
    int SUCCESS                 = 200;

    int BAD_REQUEST             = 400;
    int AUTHENTICATION_REQUIRED = 401;
    int NOT_AUTHORIZED          = 403;
}
