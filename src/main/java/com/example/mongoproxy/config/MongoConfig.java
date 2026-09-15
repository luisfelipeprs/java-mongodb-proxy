package com.example.mongoproxy.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

import java.util.concurrent.TimeUnit;

/**
 * Cria um único MongoClient (singleton) para toda a aplicação.
 * O driver do MongoDB já gerencia um pool de conexões internamente por MongoClient,
 * então basta configurar os parâmetros do pool e reutilizar essa mesma instância
 * em todas as requisições.
 */
@Factory
public class MongoConfig {

    @Value("${mongodb.uri}")
    private String mongoUri;

    @Value("${mongodb.pool.max-size:50}")
    private int maxPoolSize;

    @Value("${mongodb.pool.min-size:5}")
    private int minPoolSize;

    @Value("${mongodb.pool.max-wait-time-ms:5000}")
    private long maxWaitTimeMs;

    @Value("${mongodb.pool.max-connection-idle-time-ms:60000}")
    private long maxConnectionIdleTimeMs;

    // preDestroy garante que o client (e o pool) seja fechado corretamente
    // quando a aplicação for encerrada.
    @Bean(preDestroy = "close")
    @Singleton
    public MongoClient mongoClient() {
        ConnectionString connectionString = new ConnectionString(mongoUri);

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .applyToConnectionPoolSettings(builder -> builder
                        .maxSize(maxPoolSize)
                        .minSize(minPoolSize)
                        .maxWaitTime(maxWaitTimeMs, TimeUnit.MILLISECONDS)
                        .maxConnectionIdleTime(maxConnectionIdleTimeMs, TimeUnit.MILLISECONDS)
                )
                .build();

        return MongoClients.create(settings);
    }
}
