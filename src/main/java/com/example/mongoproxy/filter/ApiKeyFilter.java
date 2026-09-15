package com.example.mongoproxy.filter;

import io.micronaut.context.annotation.Value;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;

/**
 * Filtro simples de autenticação por API key.
 *
 * Se a propriedade "mongo-proxy.api-key" (env var PROXY_API_KEY) não for
 * definida, o filtro fica inativo (comportamento padrão, sem autenticação).
 * Se for definida, toda requisição (exceto /health) precisa enviar o header
 * "X-API-Key" com o mesmo valor.
 *
 * Isso é o mínimo recomendado já que essa aplicação vai ficar exposta na
 * rede para ser chamada pela Lambda. Para produção, prefira também restringir
 * o acesso via Security Group / VPC.
 */
@Filter("/**")
public class ApiKeyFilter implements HttpServerFilter {

    @Value("${mongo-proxy.api-key:}")
    private String expectedApiKey;

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        if (expectedApiKey == null || expectedApiKey.isBlank()) {
            return chain.proceed(request);
        }

        if (request.getPath().equals("/health")) {
            return chain.proceed(request);
        }

        String providedKey = request.getHeaders().get("X-API-Key");
        if (expectedApiKey.equals(providedKey)) {
            return chain.proceed(request);
        }

        return Publishers.just(HttpResponse.unauthorized());
    }
}
