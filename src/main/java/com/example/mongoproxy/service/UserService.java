package com.example.mongoproxy.service;

import com.example.mongoproxy.model.User;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class UserService {

    private final MongoClient mongoClient;

    @Value("${mongodb.database}")
    private String databaseName;

    @Value("${mongodb.users-collection:users}")
    private String collectionName;

    public UserService(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    public List<User> listUsers() {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection(collectionName);

        List<User> users = new ArrayList<>();
        for (Document doc : collection.find()) {
            users.add(User.fromDocument(doc));
        }
        return users;
    }
}
