package it.homegym.controller;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import it.homegym.dao.UtenteDAO;
import it.homegym.model.Utente;
import org.bson.BsonDateTime;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
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

        HttpSession session = req.getSession(false);

        try {
            MongoCollection<Document> col = mongoDb.getCollection("posts");
            Document postDoc = null;

            try {
                ObjectId oid = new ObjectId(id);
                postDoc = col.find(eq("_id", oid)).first();
            } catch (IllegalArgumentException iae) {
                postDoc = col.find(eq("_id", id)).first();
            }

            // se non trovato -> redirect con flash
            if (postDoc == null) {
                if (session != null) session.setAttribute("flashError", "Post non trovato.");
                // redirect in base al ruolo se possibile
                Utente sessUser = session != null ? (Utente) session.getAttribute("user") : null;
                if (sessUser != null && "PERSONALE".equals(sessUser.getRuolo())) {
                    resp.sendRedirect(req.getContextPath() + "/staff/community");
                } else if (sessUser != null && "CLIENTE".equals(sessUser.getRuolo())) {
                    resp.sendRedirect(req.getContextPath() + "/client/profile");
                } else {
                    resp.sendRedirect(req.getContextPath() + "/posts");
                }
                return;
            }

            // --- Normalizza _id e crea _idStr (hex) ---
            String hexId;
            if (postDoc.containsKey("_id")) {
                Object raw = postDoc.get("_id");
                if (raw instanceof ObjectId) {
                    hexId = ((ObjectId) raw).toHexString();
                } else {
                    hexId = raw != null ? raw.toString() : "";
                }
                postDoc.put("_id", hexId);
                postDoc.put("_idStr", hexId);
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

            //--- Normalize trainerEvaluations and medias arrays ---
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

            // Normalize medias array and fileId fields
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
            String visibility = postDoc.getString("visibility");
            if (visibility == null) {
                visibility = "PUBLIC";
                postDoc.put("visibility", visibility);
            }

            // --- AUTH / VISIBILITY RULES ---
            Utente user = session != null ? (Utente) session.getAttribute("user") : null;

            // ottieni author id robustamente (usato sotto)
            Integer authorId = null;
            Object maybeUid = postDoc.get("userId");
            if (maybeUid instanceof Number) {
                authorId = ((Number) maybeUid).intValue();
            } else if (maybeUid != null) {
                try { authorId = Integer.parseInt(maybeUid.toString()); } catch (NumberFormatException ignored) {}
            }

            // if hidden, allow only PROPRIETARIO or post author to view; otherwise redirect with flash
            if ("HIDDEN".equalsIgnoreCase(visibility)) {
                boolean isAdmin = user != null && "PROPRIETARIO".equals(user.getRuolo());
                boolean isAuthor = user != null && authorId != null && user.getId() == authorId;

                if (!isAdmin && !isAuthor) {
                    if (session != null) session.setAttribute("flashError", "Questo post è stato rimosso dalla community e non è visibile.");
                    if (user != null && "PERSONALE".equals(user.getRuolo())) {
                        resp.sendRedirect(req.getContextPath() + "/staff/community");
                    } else if (user != null && "CLIENTE".equals(user.getRuolo())) {
                        resp.sendRedirect(req.getContextPath() + "/client/profile");
                    } else {
                        resp.sendRedirect(req.getContextPath() + "/posts");
                    }
                    return;
                }
            }

            // If logged-in is PERSONALE, allow view only for their assigned clients (redirect with flash if not allowed)
            if (user != null && "PERSONALE".equals(user.getRuolo())) {
                if (authorId == null) {
                    if (session != null) session.setAttribute("flashError", "Autore del post non valido.");
                    resp.sendRedirect(req.getContextPath() + "/staff/community");
                    return;
                }

                try {
                    UtenteDAO udao = new UtenteDAO();
                    List<Integer> clientIds = udao.listClientIdsByTrainer(user.getId());
                    if (clientIds == null || !clientIds.contains(authorId)) {
                        if (session != null) session.setAttribute("flashError", "Non sei autorizzato a visualizzare questo post.");
                        resp.sendRedirect(req.getContextPath() + "/staff/community");
                        return;
                    }
                } catch (SQLException sqle) {
                    throw new ServletException("Errore controllo autorizzazione trainer", sqle);
                }
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
