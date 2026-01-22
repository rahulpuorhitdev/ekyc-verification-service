package org.example.ekyc.http;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.time.Duration;

public interface HttpClient {
    <TResponse> TResponse postJSON(String URL, Object request, Class<TResponse> responseType, Duration timeout);
}
