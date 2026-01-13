// ./db_init/001_create_posts_indexes.js
(function() {
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

  // Indici utili (idempotenti: createIndex non ricrea se già presente)
  tdb.posts.createIndex({ userId: 1 }, { name: 'idx_posts_userId' });
  tdb.posts.createIndex({ createdAt: -1 }, { name: 'idx_posts_createdAt' });
  tdb.posts.createIndex({ tags: 1 }, { name: 'idx_posts_tags' });
  tdb.posts.createIndex({ content: "text", "comments.text": "text" }, { name: 'text_content_comments', default_language: 'italian' });
  tdb.posts.createIndex({ "media.type": 1 }, { name: 'idx_posts_media_type' });
  tdb.posts.createIndex({ "comments.userId": 1 }, { name: 'idx_posts_comments_userId' });
  tdb.posts.createIndex({ "trainerEvaluations.trainerId": 1 }, { name: 'idx_posts_eval_trainerId' });
  tdb.posts.createIndex({ "trainerEvaluations.createdAt": -1 }, { name: 'idx_posts_eval_createdAt' });

  print('Indexes created/ensured for posts collection.');
})();
