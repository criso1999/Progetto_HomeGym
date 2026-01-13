package it.homegym.dao;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import it.homegym.util.MongoUtil;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PostDAO {
    private final MongoCollection<Document> col;

    public PostDAO() {
        col = MongoUtil.getDatabase().getCollection("posts");
        ensureIndexes();
    }

    /**
     * Garantisce indici idempotenti senza errori di conflitto nome
     */
    private void ensureIndexes() {
        List<Document> existing = col.listIndexes().into(new ArrayList<>());

        boolean createdAtExists = existing.stream().anyMatch(idx -> {
            Document key = (Document) idx.get("key");
            return key != null && key.containsKey("createdAt");
        });

        if (!createdAtExists) {
            col.createIndex(
                    Indexes.descending("createdAt"),
                    new IndexOptions().name("idx_posts_createdAt")
            );
        }

        boolean userIdExists = existing.stream().anyMatch(idx -> {
            Document key = (Document) idx.get("key");
            return key != null && key.containsKey("userId");
        });

        if (!userIdExists) {
            col.createIndex(
                    Indexes.ascending("userId"),
                    new IndexOptions().name("idx_posts_userId")
            );
        }
    }

    public ObjectId createPost(int userId, String userName, String content, List<Document> medias) {
        Document d = new Document()
                .append("userId", userId)
                .append("userName", userName)
                .append("content", content)
                .append("medias", medias == null ? new ArrayList<>() : medias)
                .append("comments", new ArrayList<>())
                .append("trainerEvaluations", new ArrayList<>())
                .append("createdAt", Date.from(Instant.now()))
                .append("updatedAt", Date.from(Instant.now()))
                .append("visibility", "PUBLIC");

        col.insertOne(d);
        return d.getObjectId("_id");
    }

    public List<Document> listFeed(int page, int pageSize) {
        int skip = (page - 1) * pageSize;

        FindIterable<Document> it = col.find()
                .sort(Indexes.descending("createdAt"))
                .skip(skip)
                .limit(pageSize);

        List<Document> out = new ArrayList<>();
        for (Document d : it) out.add(d);
        return out;
    }

    public Document findById(ObjectId id) {
        return col.find(Filters.eq("_id", id)).first();
    }

    public void addComment(ObjectId postId, int userId, String userName, String text) {
        Document comment = new Document("_id", new ObjectId())
                .append("userId", userId)
                .append("userName", userName)
                .append("text", text)
                .append("createdAt", Date.from(Instant.now()));

        col.updateOne(
                Filters.eq("_id", postId),
                new Document("$push", new Document("comments", comment))
                        .append("$set", new Document("updatedAt", Date.from(Instant.now())))
        );
    }

    public void addTrainerEvaluation(ObjectId postId, int trainerId, String trainerName, String note, int score) {
        Document ev = new Document("_id", new ObjectId())
                .append("trainerId", trainerId)
                .append("trainerName", trainerName)
                .append("note", note)
                .append("score", score)
                .append("createdAt", Date.from(Instant.now()));

        col.updateOne(
                Filters.eq("_id", postId),
                new Document("$push", new Document("trainerEvaluations", ev))
                        .append("$set", new Document("updatedAt", Date.from(Instant.now())))
        );
    }

    public boolean deletePost(ObjectId id) {
        return col.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
    }
}
