package sample;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;

public class Database {
    MongoClient mongo = new MongoClient( "localhost" , 27017 );
    MongoDatabase database = mongo.getDatabase("myServer");

    MongoCollection<Document> collectionUsers = database.getCollection("users");
    MongoCollection<Document> collectionLogs = database.getCollection("log");
    MongoCollection<Document> collectionMessages = database.getCollection("messages");

    public void insertUser(JSONObject jsonObject){
        Document document = new Document();
        document.append("firstName", jsonObject.getString("firstName"));
        document.append("lastName", jsonObject.getString("lastName"));
        document.append("login", jsonObject.getString("login"));
        document.append("password", jsonObject.getString("password"));
        collectionUsers.insertOne(document);
    }

    public void logLogout(JSONObject jsonObject){
        Document document = new Document();
        document.append("type", "logout");
        document.append("login", jsonObject.getString("login"));
        document.append("datetime", jsonObject.getString("datetime"));
        collectionLogs.insertOne(document);
    }

    public void logLogin(JSONObject jsonObject){
        Document document = new Document();
        document.append("type", "login");
        document.append("login", jsonObject.getString("login"));
        document.append("datetime", jsonObject.getString("datetime"));
        collectionLogs.insertOne(document);
    }

    public void insertMessage(JSONObject jsonObject){
        Document document = new Document();
        document.append("from", jsonObject.getString("from"));
        document.append("message", jsonObject.getString("message"));
        document.append("to", jsonObject.getString("to"));
        collectionMessages.insertOne(document);
    }

    public void deleteUser(String login){
        BasicDBObject theQuery = new BasicDBObject();
        theQuery.put("login", login);
        collectionUsers.deleteOne(theQuery);

        try (MongoCursor<Document> cursor = collectionMessages.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (object.getString("from").equals(login)){
                    theQuery = new BasicDBObject();
                    theQuery.put("from", login);
                    collectionMessages.deleteOne(theQuery);
                }
            }
        }
    }
    
    public void updateFName(String name, String firstName) {
        Bson filter = new Document("firstName", name);
        Bson newValue = new Document("firstName", firstName);
        Bson updateOperationDocument = new Document("$set", newValue);
        collectionUsers.updateOne(filter, updateOperationDocument);
    }

    public void updateLName(String name, String lastName) {
        Bson filter = new Document("lastName", name);
        Bson newValue = new Document("lastName", lastName);
        Bson updateOperationDocument = new Document("$set", newValue);
        collectionUsers.updateOne(filter, updateOperationDocument);
    }

    public boolean matchLogin(String login, String password) {
        try (MongoCursor<Document> cursor = collectionUsers.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (object.getString("login").equals(login) && BCrypt.checkpw(password, object.getString("password"))){
                    return true;
                }
            }
        }
        return false;
    }

    public void login(String login, String token) {
        try (MongoCursor<Document> cursor = collectionUsers.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (login.equals(object.getString("login"))){
                    Document filterDoc = new Document().append("login", login);
                    Document updateDoc = new Document().append("$set", new Document().append("token", token));
                    collectionUsers.updateOne(filterDoc, updateDoc);
                }
            }
        }
    }

    public void logout(String login, String token) {
        try (MongoCursor<Document> cursor = collectionUsers.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (login.equals(object.getString("login"))){
                    Document filterDoc = new Document().append("login", login);
                    Document updateDoc = new Document().append("$unset", new Document().append("token", token));
                    collectionUsers.updateOne(filterDoc, updateDoc);
                }
            }
        }
    }

    public JSONObject getUser(String login) {
        try (MongoCursor<Document> cursor = collectionUsers.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (object.getString("login").equals(login)){
                    return object;
                }
            }
        }
        return null;
    }

    public boolean findToken(String token) {
        try (MongoCursor<Document> cursor = collectionUsers.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (object.getString("token").equals(token)){
                    return true;
                }
            }
        }
        return false;
    }
}
