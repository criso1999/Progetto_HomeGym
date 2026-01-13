package it.homegym.dao;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import it.homegym.util.MongoUtil;
import org.bson.types.ObjectId;

import java.io.InputStream;
import java.io.OutputStream;

public class MediaDAO {

    private final GridFSBucket bucket;

    public MediaDAO() {
        this.bucket = GridFSBuckets.create(MongoUtil.getDatabase());
    }

    public ObjectId upload(InputStream in, String filename, String contentType) {
        GridFSUploadOptions opts = new GridFSUploadOptions()
                .metadata(new org.bson.Document("contentType", contentType));
        return bucket.uploadFromStream(filename, in, opts);
    }

    public void streamTo(ObjectId id, OutputStream out) {
        bucket.downloadToStream(id, out);
    }
}
