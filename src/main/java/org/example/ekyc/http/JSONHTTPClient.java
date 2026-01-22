package org.example.ekyc.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ekyc.exception.HttpStatusServiceException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class JSONHTTPClient implements HttpClient {
    private final ObjectMapper mapper;
    private final java.net.http.HttpClient client;
    private final String serviceName;

    public JSONHTTPClient(ObjectMapper mapper1, java.net.http.HttpClient client, String serviceName) {
        this.mapper = mapper1;
        this.client = client;
        this.serviceName = serviceName;
    }

    @Override
    public <TResponse> TResponse postJSON(String URL, Object request, Class<TResponse> responseType, Duration timeout){
        try{
            String json = mapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(URL))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json)).build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            final int code = response.statusCode();

            if(code >= 200 && code < 300) {
                return mapper.readValue(response.body(), responseType);
            }

            throw new HttpStatusServiceException(serviceName, "HTTP response code " + code, code);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
