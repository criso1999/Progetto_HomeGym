package it.homegym.controller;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.BsonDateTime;
import org.bson.types.ObjectId;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

@WebServlet("/posts/view")
public class PostsViewServlet extends HttpServlet {

    private MongoClient mongoClient;
    private MongoDatabase mongoDb;

    @Override
    public void init() throws ServletException {
        super.init();
        String uri = System.getenv("MONGO_URI");
        if (uri == null || uri.isBlank()) {
            String host = System.getenv().getOrDefault("MONGO_HOST", "localhost");
            String port = System.getenv().getOrDefault("MONGO_PORT", "27017");
            String db = System.getenv().getOrDefault("MONGO_DB", "homegym_mongo");
            uri = "mongodb://" + host + ":" + port;
            mongoClient = MongoClients.create(uri);
            mongoDb = mongoClient.getDatabase(db);
        } else {
            mongoClient = MongoClients.create(uri);
            String db = System.getenv().getOrDefault("MONGO_DB", "homegym_mongo");
            mongoDb = mongoClient.getDatabase(db);
        }

        System.out.println("PostsViewServlet: connected to mongo db: " + mongoDb.getName());
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String id = req.getParameter("id");
        if (id == null || id.isBlank()) {
            resp.sendRedirect(req.getContextPath() + "/posts");
            return;
        }

        try {
            MongoCollection<Document> col = mongoDb.getCollection("posts");
            Document postDoc = null;

            try {
                ObjectId oid = new ObjectId(id);
                postDoc = col.find(eq("_id", oid)).first();
            } catch (IllegalArgumentException iae) {
                postDoc = col.find(eq("_id", id)).first();
            }

            if (postDoc == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                req.setAttribute("error", "Post non trovato");
                req.getRequestDispatcher("/WEB-INF/views/posts/view.jsp").forward(req, resp);
                return;
            }

            // --- Normalizza _id e crea _idStr (hex) ---
            if (postDoc.containsKey("_id")) {
                Object raw = postDoc.get("_id");
                String hex;
                if (raw instanceof ObjectId) {
                    hex = ((ObjectId) raw).toHexString();
                } else {
                    hex = raw != null ? raw.toString() : "";
                }
                postDoc.put("_id", hex);
                postDoc.put("_idStr", hex);
            } else {
                postDoc.put("_idStr", "");
            }

            // --- Normalize createdAt top-level to java.util.Date ---
            if (postDoc.containsKey("createdAt")) {
                Object c = postDoc.get("createdAt");
                if (c instanceof Date) {
                    // ok
                } else if (c instanceof BsonDateTime) {
                    postDoc.put("createdAt", new Date(((BsonDateTime) c).getValue()));
                } else if (c instanceof Number) {
                    postDoc.put("createdAt", new Date(((Number) c).longValue()));
                } // otherwise leave as-is
            }

            // --- Ensure arrays exist and normalize nested createdAt fields ---
            Object cmMaybe = postDoc.get("comments");
            if (!(cmMaybe instanceof List)) {
                postDoc.put("comments", new ArrayList<>());
            } else {
                List<Document> comments = (List<Document>) cmMaybe;
                for (Document cm : comments) {
                    Object cc = cm.get("createdAt");
                    if (cc instanceof BsonDateTime) {
                        cm.put("createdAt", new Date(((BsonDateTime) cc).getValue()));
                    } else if (cc instanceof Number) {
                        cm.put("createdAt", new Date(((Number) cc).longValue()));
                    }
                }
            }

            Object evMaybe = postDoc.get("trainerEvaluations");
            if (!(evMaybe instanceof List)) {
                postDoc.put("trainerEvaluations", new ArrayList<>());
            } else {
                List<Document> evals = (List<Document>) evMaybe;
                for (Document ev : evals) {
                    Object ec = ev.get("createdAt");
                    if (ec instanceof BsonDateTime) {
                        ev.put("createdAt", new Date(((BsonDateTime) ec).getValue()));
                    } else if (ec instanceof Number) {
                        ev.put("createdAt", new Date(((Number) ec).longValue()));
                    }
                }
            }

            Object mdMaybe = postDoc.get("medias");
            if (!(mdMaybe instanceof List)) {
                postDoc.put("medias", new ArrayList<>());
            } else {
                List<Document> medias = (List<Document>) mdMaybe;
                for (Document m : medias) {
                    Object fid = m.get("fileId");
                    if (fid instanceof ObjectId) {
                        m.put("fileId", ((ObjectId) fid).toHexString());
                    } else if (fid != null) {
                        m.put("fileId", fid.toString());
                    }
                }
            }

            // default visibility if missing
            if (!postDoc.containsKey("visibility")) {
                postDoc.put("visibility", "PUBLIC");
            }

            // pass to JSP
            req.setAttribute("post", postDoc);
            req.getRequestDispatcher("/WEB-INF/views/posts/view.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException("Errore recupero post da Mongo", e);
        }
    }

    @Override
    public void destroy() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
                System.out.println("PostsViewServlet: mongo client closed");
            } catch (Exception ignored) {}
        }
        super.destroy();
    }
}
