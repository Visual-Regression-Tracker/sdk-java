package io.visual_regression_tracker.sdk_java;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class MockHttpClient extends HttpClient {

    private final String body;
    private int statusCode;

    public MockHttpClient(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    @Override
    public Optional<CookieHandler> cookieHandler() {
        return null;
    }

    @Override
    public Optional<Duration> connectTimeout() {
        return null;
    }

    @Override
    public Redirect followRedirects() {
        return null;
    }

    @Override
    public Optional<ProxySelector> proxy() {
        return null;
    }

    @Override
    public SSLContext sslContext() {
        return null;
    }

    @Override
    public SSLParameters sslParameters() {
        return null;
    }

    @Override
    public Optional<Authenticator> authenticator() {
        return null;
    }

    @Override
    public Version version() {
        return null;
    }

    @Override
    public Optional<Executor> executor() {
        return null;
    }

    @Override
    public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
        HttpResponse httpResponse = new HttpResponse() {
            @Override
            public int statusCode() {
                return statusCode;
            }

            @Override
            public HttpRequest request() {
                return null;
            }

            @Override
            public Optional<HttpResponse> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                return null;
            }

            @Override
            public Object body() {
                return body;
            }

            @Override
            public Optional<SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public URI uri() {
                return null;
            }

            @Override
            public Version version() {
                return null;
            }
        };
        return httpResponse;
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
        return null;
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>>
    sendAsync(HttpRequest x, HttpResponse.BodyHandler<T> y, HttpResponse.PushPromiseHandler<T> z) {
        return null;
    }
}
