package it.homegym.controller;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Date;

import static com.mongodb.client.model.Filters.eq;

@WebServlet("/posts/view")
public class PostsViewServlet extends HttpServlet {

    private MongoClient mongoClient;
    private MongoDatabase mongoDb;

    @Override
    public void init() throws ServletException {
        super.init();
        // Prefer MONGO_URI (e.g. mongodb://user:pass@host:port/db), altrimenti costruisci dalla singole env
        String uri = System.getenv("MONGO_URI");
        if (uri == null || uri.isBlank()) {
            String host = System.getenv().getOrDefault("MONGO_HOST", "localhost");
            String port = System.getenv().getOrDefault("MONGO_PORT", "27017");
            String db = System.getenv().getOrDefault("MONGO_DB", "homegym_mongo");
            uri = "mongodb://" + host + ":" + port;
            // connessione senza auth. Se usi user/pass, fornisci MONGO_URI.
            mongoClient = MongoClients.create(uri);
            mongoDb = mongoClient.getDatabase(db);
        } else {
            // se MONGO_URI include il db, possiamo comunque selezionare con MONGO_DB override
            mongoClient = MongoClients.create(uri);
            String db = System.getenv().getOrDefault("MONGO_DB", "homegym_mongo");
            mongoDb = mongoClient.getDatabase(db);
        }

        System.out.println("PostsViewServlet: connected to mongo db: " + mongoDb.getName());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String id = req.getParameter("id");
        if (id == null || id.isBlank()) {
            // nessun id -> redirect alla lista post
            resp.sendRedirect(req.getContextPath() + "/posts");
            return;
        }

        try {
            MongoCollection<Document> col = mongoDb.getCollection("posts");
            Document postDoc = null;

            // prova a convertire in ObjectId (hex string), altrimenti ricerca per campo _id come stringa
            try {
                ObjectId oid = new ObjectId(id);
                postDoc = col.find(eq("_id", oid)).first();
            } catch (IllegalArgumentException iae) {
                // id non è un ObjectId valido: prova a cercare per campo "_id" come stringa
                postDoc = col.find(eq("_id", id)).first();
            }

            if (postDoc == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                req.setAttribute("error", "Post non trovato");
                req.getRequestDispatcher("/WEB-INF/views/posts/view.jsp").forward(req, resp);
                return;
            }

            // Normalizza _id: metti una hex string in _id (utile per la JSP / forms)
            if (postDoc.containsKey("_id")) {
                Object raw = postDoc.get("_id");
                if (raw instanceof ObjectId) {
                    postDoc.put("_id", ((ObjectId) raw).toHexString());
                } else {
                    // lascia com'è (stringa o altro)
                    postDoc.put("_id", raw.toString());
                }
            }

            // Se vuoi che createdAt sia un java.util.Date (fmt:formatDate funziona con Date)
            if (postDoc.containsKey("createdAt")) {
                Object c = postDoc.get("createdAt");
                if (c instanceof Date) {
                    // ok
                } else if (c instanceof org.bson.BsonDateTime) {
                    postDoc.put("createdAt", new Date(((org.bson.BsonDateTime) c).getValue()));
                } // altrimenti lascia
            }

            // assicurati che array comments/trainerEvaluations/medias esistano (opzionale)
            // (JSP gestirà "empty" correttamente)

            // passa il document alla JSP
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
