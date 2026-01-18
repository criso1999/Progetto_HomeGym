package it.homegym.dao;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import it.homegym.util.MongoUtil;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Filters.*;

/**
 * DAO per la collection "posts".
 * - gestisce creazione post, listing, commenti, valutazioni, hide/restore, delete.
 * - garantisce indici idempotenti in ensureIndexes().
 */
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

        // createdAt desc
        boolean createdAtExists = existing.stream().anyMatch(idx -> {
            Document key = (Document) idx.get("key");
            return key != null && key.containsKey("createdAt");
        });
        if (!createdAtExists) {
            col.createIndex(Indexes.descending("createdAt"), new IndexOptions().name("idx_posts_createdAt"));
        }

        // userId asc
        boolean userIdExists = existing.stream().anyMatch(idx -> {
            Document key = (Document) idx.get("key");
            return key != null && key.containsKey("userId");
        });
        if (!userIdExists) {
            col.createIndex(Indexes.ascending("userId"), new IndexOptions().name("idx_posts_userId"));
        }

        // visibility + createdAt (filtraggio feed)
        boolean visIdx = existing.stream().anyMatch(idx -> {
            Document key = (Document) idx.get("key");
            return key != null && key.containsKey("visibility") && key.containsKey("createdAt");
        });
        if (!visIdx) {
            col.createIndex(Indexes.compoundIndex(Indexes.ascending("visibility"), Indexes.descending("createdAt")),
                    new IndexOptions().name("idx_posts_visibility_createdAt"));
        }

        // hiddenBy, hiddenAt (audit)
        boolean hiddenAudit = existing.stream().anyMatch(idx -> {
            Document key = (Document) idx.get("key");
            return key != null && key.containsKey("hiddenBy");
        });
        if (!hiddenAudit) {
            col.createIndex(Indexes.compoundIndex(Indexes.ascending("hiddenBy"), Indexes.descending("hiddenAt")),
                    new IndexOptions().name("idx_posts_hiddenBy_hiddenAt"));
        }

        // text index on content + comments.text
        boolean textExists = existing.stream().anyMatch(idx -> {
            Document key = (Document) idx.get("key");
            return key != null && (key.containsKey("content") || key.containsKey("comments.text"));
        });
        if (!textExists) {
            col.createIndex(new Document("content", "text").append("comments.text", "text"),
                    new IndexOptions().name("text_content_comments").defaultLanguage("italian"));
        }

        // medias contentType
        boolean mediaIdx = existing.stream().anyMatch(idx -> {
            Document key = (Document) idx.get("key");
            return key != null && key.containsKey("medias.contentType");
        });
        if (!mediaIdx) {
            col.createIndex(Indexes.ascending("medias.contentType"), new IndexOptions().name("idx_posts_media_contentType"));
        }

        // comments.userId index
        boolean commentsUserIdx = existing.stream().anyMatch(idx -> {
            Document key = (Document) idx.get("key");
            return key != null && key.containsKey("comments.userId");
        });
        if (!commentsUserIdx) {
            col.createIndex(Indexes.ascending("comments.userId"), new IndexOptions().name("idx_posts_comments_userId"));
        }

        // trainerEvaluations.trainerId
        boolean evalTrainerIdx = existing.stream().anyMatch(idx -> {
            Document key = (Document) idx.get("key");
            return key != null && key.containsKey("trainerEvaluations.trainerId");
        });
        if (!evalTrainerIdx) {
            col.createIndex(Indexes.ascending("trainerEvaluations.trainerId"), new IndexOptions().name("idx_posts_eval_trainerId"));
        }
    }

    // Crea un nuovo post e ritorna il suo ObjectId
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

    /**
     * Nasconde un post (soft-delete / moderazione).
     * @return true se effettivamente aggiornato (modifiedCount > 0)
     */
    public boolean hidePost(ObjectId postId, int adminId, String reason) {
        UpdateResult res = col.updateOne(
                Filters.eq("_id", postId),
                Updates.combine(
                        Updates.set("visibility", "HIDDEN"),
                        Updates.set("hiddenBy", adminId),
                        Updates.set("hiddenReason", reason == null ? "" : reason),
                        Updates.set("hiddenAt", new Date()),
                        Updates.set("updatedAt", new Date())
                )
        );
        return res.getModifiedCount() > 0;
    }

    /**
     * Ripristina la visibilità di un post rimuovendo i campi di moderazione.
     */
    public boolean restorePost(ObjectId postId) {
        UpdateResult res = col.updateOne(
                Filters.eq("_id", postId),
                Updates.combine(
                        Updates.set("visibility", "PUBLIC"),
                        Updates.set("updatedAt", new Date()),
                        Updates.unset("hiddenBy"),
                        Updates.unset("hiddenReason"),
                        Updates.unset("hiddenAt")
                )
        );
        return res.getModifiedCount() > 0;
    }

    // Elenca i post pubblici per il feed, con paginazione
    public List<Document> listFeedPublic(int page, int pageSize) {
        return listVisibleByUserIdsInternal(null, false, page, pageSize);
    }

    // Elenca i post pubblici creati dagli utenti specificati, con paginazione
    public List<Document> listVisibleByUserIds(List<Integer> userIds, int page, int pageSize) {
        return listVisibleByUserIdsInternal(userIds, false, page, pageSize);
    }

    /**
     * Metodo interno per listare post filtrando per userIds e visibilità.
     * @param userIds lista di userId (null = tutti)
     * @param includeHidden se true include anche post con visibility != PUBLIC
     */
    private List<Document> listVisibleByUserIdsInternal(List<Integer> userIds, boolean includeHidden, int page, int pageSize) {
        int skip = Math.max(0, (page - 1)) * pageSize;
        List<Document> out = new ArrayList<>();

        // costruisci filtro
        org.bson.conversions.Bson filter = null;
        if (userIds != null && !userIds.isEmpty()) {
            if (includeHidden) {
                filter = in("userId", userIds);
            } else {
                filter = and(in("userId", userIds), eq("visibility", "PUBLIC"));
            }
        } else {
            if (includeHidden) {
                filter = new Document(); // match all
            } else {
                filter = eq("visibility", "PUBLIC");
            }
        }

        FindIterable<Document> it = col.find(filter)
                .sort(Sorts.descending("createdAt"))
                .skip(skip)
                .limit(pageSize);

        for (Document d : it) out.add(d);
        return out;
    }

    // Elenca tutti i post per l'admin, con paginazione (inclusi Hidden)
    public List<Document> listAllForAdmin(int page, int pageSize) {
        int skip = Math.max(0, (page - 1)) * pageSize;

        FindIterable<Document> it = col.find()
                .sort(Sorts.descending("createdAt"))
                .skip(skip)
                .limit(pageSize);

        List<Document> out = new ArrayList<>();
        for (Document d : it) out.add(d);
        return out;
    }

    public Document findById(ObjectId id) {
        return col.find(eq("_id", id)).first();
    }

    // Aggiunge un commento a un post
    public void addComment(ObjectId postId, int userId, String userName, String text) {
        Document comment = new Document("_id", new ObjectId())
                .append("userId", userId)
                .append("userName", userName)
                .append("text", text)
                .append("createdAt", new Date());

        col.updateOne(
                eq("_id", postId),
                Updates.combine(
                        Updates.push("comments", comment),
                        Updates.set("updatedAt", new Date())
                )
        );
    }

    /**
     * Aggiunge una valutazione fatta da un trainer. Ritorna true se aggiunta, false se già esiste.
     */
    public boolean addTrainerEvaluation(ObjectId postId, int trainerId, String trainerName, String note, int score) {
        Document ev = new Document("_id", new ObjectId())
                .append("trainerId", trainerId)
                .append("trainerName", trainerName)
                .append("note", note)
                .append("score", score)
                .append("createdAt", new Date());

        // filtro: post con dato _id e senza una valutazione di questo trainer
        org.bson.conversions.Bson filter = and(eq("_id", postId), Filters.not(Filters.elemMatch("trainerEvaluations", eq("trainerId", trainerId))));

        UpdateResult res = col.updateOne(filter,
                Updates.combine(
                        Updates.push("trainerEvaluations", ev),
                        Updates.set("updatedAt", new Date())
                ));
        return res.getModifiedCount() > 0;
    }

    public boolean hasTrainerEvaluation(ObjectId postId, int trainerId) {
        Document found = col.find(and(eq("_id", postId), Filters.elemMatch("trainerEvaluations", eq("trainerId", trainerId)))).first();
        return found != null;
    }

    // Cancellazione definitiva (hard delete)
    public boolean deletePost(ObjectId id) {
        return col.deleteOne(eq("_id", id)).getDeletedCount() > 0;
    }
}
