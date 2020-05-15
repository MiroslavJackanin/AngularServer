package sample;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.mindrot.jbcrypt.BCrypt;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private final JSONObject config = getConfig();
    private final String host = config.getString("host");
    private final int port = Integer.parseInt(config.getString("port"));
    private final String dbName = config.getString("dbname");

    MongoClient mongo = new MongoClient(host, port);
    MongoDatabase database = mongo.getDatabase(dbName);

    MongoCollection<Document> collectionUsers = database.getCollection("users");
    MongoCollection<Document> collectionLogs = database.getCollection("log");
    MongoCollection<Document> collectionMessages = database.getCollection("messages");

    public JSONObject getConfig(){
        JSONObject config = new JSONObject();
        JSONParser parser = new JSONParser();
        try {
            org.json.simple.JSONObject obj = (org.json.simple.JSONObject) parser.parse(new FileReader("src/main/java/sample/configuration"));

            config.put("url", obj.get("url"));
            config.put("port", obj.get("port"));
            config.put("dbname", obj.get("dbname"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return config;
    }

    public void insertUser(JSONObject jsonObject){
        Document document = new Document();
        document.append("firstName", jsonObject.getString("firstName"));
        document.append("lastName", jsonObject.getString("lastName"));
        document.append("login", jsonObject.getString("login"));
        document.append("password", jsonObject.getString("password"));
        collectionUsers.insertOne(document);
    }

    public void logLogout(JSONObject jsonObject){
        System.out.println("here log logout");
        Document document = new Document();
        document.append("type", "logout");
        document.append("login", jsonObject.getString("login"));
        document.append("datetime", jsonObject.getString("datetime"));
        collectionLogs.insertOne(document);
    }

    public void logLogin(JSONObject jsonObject){
        System.out.println("here log login");
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

    public List<JSONObject> getLoggedUsers() {
        List<JSONObject> list = new ArrayList<>();
        try (MongoCursor<Document> cursor = collectionUsers.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (object.has("token")){
                    list.add(object);
                }
            }
        }
        return list;
    }

    public JSONObject getLoggedUser(String userLogin) {
        JSONObject user = new JSONObject();
        try (MongoCursor<Document> cursor = collectionUsers.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (object.getString("login").equals(userLogin)){
                    user = object;
                }
            }
        }
        return user;
    }

    public boolean matchToken(String login, String token) {
        try (MongoCursor<Document> cursor = collectionUsers.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (object.getString("login").equals(login) && object.getString("token").equals(token)){
                    return true;
                }
            }
        }
        return false;
    }

    public void changePassword(String login, String passHash) {
        Bson filter = new Document("login", login);
        Bson newValue = new Document("password", passHash);
        Bson updateOperationDocument = new Document("$set", newValue);
        collectionUsers.updateOne(filter, updateOperationDocument);
    }

    public String getLogin(String token) {
        try (MongoCursor<Document> cursor = collectionUsers.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (object.getString("token").equals(token)){
                    return object.getString("login");
                }
            }
        }
        return null;
    }

    public List<String> getLogs(String login, String logType) {
        List<String> userLog = new ArrayList<>();
        try (MongoCursor<Document> cursor = collectionLogs.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (object.getString("type").equals(logType) && object.getString("login").equals(login)){
                    userLog.add(object.toString());
                }
            }
        }
        return userLog;
    }

    public boolean findLogin(String login) {
        try (MongoCursor<Document> cursor = collectionUsers.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (object.getString("login").equals(login)){
                    return true;
                }
            }
        }
        return false;
    }
}
