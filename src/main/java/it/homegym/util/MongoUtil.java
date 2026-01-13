package it.homegym.util;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public final class MongoUtil {
    private static final MongoClient client;
    private static final MongoDatabase db;

    static {
        String host = System.getenv().getOrDefault("MONGO_HOST", "localhost");
        String port = System.getenv().getOrDefault("MONGO_PORT", "27017");
        String name = System.getenv().getOrDefault("MONGO_DB", "homegym_mongo");

        String conn = System.getenv("MONGO_URI");
        if (conn == null || conn.isBlank()) {
            // semplice connection string
            conn = "mongodb://" + host + ":" + port;
        }
        ConnectionString cs = new ConnectionString(conn);
        client = MongoClients.create(cs);
        db = client.getDatabase(name);
    }

    private MongoUtil() {}

    public static MongoDatabase getDatabase() {
        return db;
    }

    public static MongoClient getClient() {
        return client;
    }
}
