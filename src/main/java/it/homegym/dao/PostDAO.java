package it.homegym.dao;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.UpdateResult;
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

    public List<Document> listByUserIds(List<Integer> userIds, int page, int pageSize) {
        if (userIds == null || userIds.isEmpty()) return new ArrayList<>();
        int skip = Math.max(0, (page - 1)) * pageSize;
        FindIterable<Document> it = col.find(Filters.in("userId", userIds))
                .sort(Sorts.descending("createdAt"))
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

        /**
     * Verifica se il trainer ha già inserito una valutazione per questo post.
     */
    public boolean hasTrainerEvaluation(ObjectId postId, int trainerId) {
        Document found = col.find(Filters.and(
                Filters.eq("_id", postId),
                Filters.elemMatch("trainerEvaluations", Filters.eq("trainerId", trainerId))
        )).first();
        return found != null;
    } 

    /**
     * Aggiunge una valutazione fatta da un trainer. Ritorna true se aggiunta, false se già esiste.
     */
    public boolean addTrainerEvaluation(ObjectId postId, int trainerId, String trainerName, String note, int score) {
        // controllo duplicato atomico: aggiungiamo solo se non esiste valutazione dello stesso trainer
        Document ev = new Document("_id", new ObjectId())
                .append("trainerId", trainerId)
                .append("trainerName", trainerName)
                .append("note", note)
                .append("score", score)
                .append("createdAt", Date.from(Instant.now()));

        // filtro: post con dato _id e senza una valutazione di questo trainer
        Document filter = new Document("_id", postId)
                .append("trainerEvaluations.trainerId", new Document("$ne", trainerId));

        Document update = new Document("$push", new Document("trainerEvaluations", ev))
                .append("$set", new Document("updatedAt", Date.from(Instant.now())));

        UpdateResult res = col.updateOne(filter, update);
        return res.getModifiedCount() > 0;
    }

    public boolean deletePost(ObjectId id) {
        return col.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
    }
}
