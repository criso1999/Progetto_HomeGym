// ./db_init/001_create_posts_indexes.js
(function () {
  const TARGET_DB = 'homegym_mongo';
  const tdb = db.getSiblingDB(TARGET_DB);
  print('==> Running posts indexes init on db: ' + tdb.getName());

  // crea collection se non esiste
  if (!tdb.getCollectionNames().includes('posts')) {
    tdb.createCollection('posts');
    print('Created collection: posts');
  } else {
    print('Collection posts already exists');
  }

  const posts = tdb.posts;

  /* ===========================
     INDICI BASE
     =========================== */
  posts.createIndex(
    { userId: 1 },
    { name: 'idx_posts_userId' }
  );

  posts.createIndex(
    { createdAt: -1 },
    { name: 'idx_posts_createdAt' }
  );

  /* ===========================
     VISIBILITÀ / MODERAZIONE
     =========================== */

  // Filtraggio feed PUBLIC / HIDDEN
  posts.createIndex(
    { visibility: 1, createdAt: -1 },
    { name: 'idx_posts_visibility_createdAt' }
  );

  // Audit moderazione admin
  posts.createIndex(
    { hiddenBy: 1, hiddenAt: -1 },
    { name: 'idx_posts_hiddenBy_hiddenAt' }
  );

  /* ===========================
     FULL TEXT SEARCH
     =========================== */
  posts.createIndex(
    {
      content: "text",
      "comments.text": "text"
    },
    {
      name: 'text_content_comments',
      default_language: 'italian'
    }
  );

  /* ===========================
     MEDIA
     =========================== */
  posts.createIndex(
    { "medias.contentType": 1 },
    { name: 'idx_posts_media_contentType' }
  );

  /* ===========================
     COMMENTI
     =========================== */
  posts.createIndex(
    { "comments.userId": 1 },
    { name: 'idx_posts_comments_userId' }
  );

  posts.createIndex(
    { "comments.createdAt": -1 },
    { name: 'idx_posts_comments_createdAt' }
  );

  /* ===========================
     TRAINER EVALUATIONS
     =========================== */
  posts.createIndex(
    { "trainerEvaluations.trainerId": 1 },
    { name: 'idx_posts_eval_trainerId' }
  );

  posts.createIndex(
    { "trainerEvaluations.createdAt": -1 },
    { name: 'idx_posts_eval_createdAt' }
  );

  print('✔ Indexes created / ensured for posts collection (moderation enabled)');
})();
