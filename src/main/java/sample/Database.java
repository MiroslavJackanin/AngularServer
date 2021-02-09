package sample;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Database {
    private MongoClient mongo = new MongoClient("localhost", 27017);
    private MongoDatabase database = mongo.getDatabase("javaServer");

    private final MongoCollection<Document> collectionUsers = database.getCollection("users");
    private final MongoCollection<Document> collectionLogs = database.getCollection("loginHistory");
    private final MongoCollection<Document> collectionMessages = database.getCollection("messages");


    public void closeDatabase() {
        this.mongo = null;
        this.database = null;
    }

    public void insertUser(JSONObject jsonObject) throws JSONException {
        Document document = new Document()
                .append("fname", jsonObject.getString("fname"))
                .append("lname", jsonObject.getString("lname"))
                .append("login", jsonObject.getString("login"))
                .append("password", jsonObject.getString("password"))
                .append("token", jsonObject.getString("token"));

        collectionUsers.insertOne(document);
    }

    public void insertMessage(JSONObject jsonObject){
        Document document = new Document();
        document.append("from", jsonObject.getString("from"));
        document.append("message", jsonObject.getString("message"));
        document.append("to", jsonObject.getString("to"));
        document.append("time", jsonObject.getString("time"));
        collectionMessages.insertOne(document);
    }

    public boolean existLogin(String login) throws JSONException {
        Document found = collectionUsers.find(new Document("login", login)).first();
        return found != null;
    }


    public JSONObject getUser(String login) throws JSONException {
        Document found = collectionUsers.find(new Document("login", login)).first();
        JSONObject object = new JSONObject(found);

        if (found == null) {
            return null;
        } else {
            return object;
        }
    }

    public List<String> getLoginHistory(String login) throws JSONException {
        List<String> loginHistory = new ArrayList<>();
        for (Document document : collectionLogs.find()) {
            JSONObject object = new JSONObject(document.toJson());  // document to json
            if (object.getString("login").equals(login)) {
                loginHistory.add(object.toString());
            }
        }
        return loginHistory;
    }

    public List<String> getAllMessages() throws JSONException {

        List<String> loginHistory = new ArrayList<>();

        for (Document document : collectionMessages.find()) {
            JSONObject object = new JSONObject(document.toJson());  // document to json
            loginHistory.add(object.toString());
        }
        return loginHistory;
    }

    public List<String> getUsers() throws JSONException {

        JSONObject userListJson = new JSONObject();
        JSONArray usersJsonArray = new JSONArray();

        List<String> usersList = new ArrayList<>();

        for (Document document : collectionUsers.find()) {
            JSONObject object = new JSONObject(document.toJson());

            usersList.add(object.getString("login"));

            userListJson.put("login", object.getString("login"));
            usersJsonArray.put(userListJson);
        }
        return usersList;
    }

    public void saveToken(String login, String token) {
        Bson updateQuery = new Document("login", login);
        Bson newValue = new Document("token", token);
        Bson update = new Document("$set", newValue);
        collectionUsers.updateOne(updateQuery, update);
    }

    public void saveLoginHistory(JSONObject jsonObject) throws JSONException {

        Document document = new Document()
                .append("type", jsonObject.getString("type"))
                .append("login", jsonObject.getString("login"))
                .append("datetime", jsonObject.getString("datetime"));

        collectionLogs.insertOne(document);
    }

    public boolean existToken(String token, String login) throws JSONException {
        try (MongoCursor<Document> cursor = collectionUsers.find().iterator()) {

            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());

                if (object.getString("login").equals(login) && object.getString("token").equals(token)) {
                    return true;
                }
            }
        }
        return false;
    }


    public void deleteToken(String login) {

        Bson updateQuery = new Document("login", login);
        Bson newValue = new Document("token", "");
        Bson update = new Document("$set", newValue);
        collectionUsers.updateOne(updateQuery, update);
    }


    public void updatePassword(String login, String hash) {
        Bson updateQuery = new Document("login", login);
        Bson newValue = new Document("password", hash);
        Bson update = new Document("$set", newValue);
        collectionUsers.updateOne(updateQuery, update);
    }

    public void deleteUser(String login, String token) throws JSONException {
        for (Document document : collectionUsers.find()) {
            JSONObject object = new JSONObject(document.toJson());

            if (object.getString("login").equals(login) && object.getString("token").equals(token)) {

                BasicDBObject deleteQuery = new BasicDBObject();
                deleteQuery.put("login", login);
                collectionUsers.deleteOne(deleteQuery);
            }
        }
    }

    public boolean deleteMessage(String time, String message, String login) {
        BasicDBObject theQuery;

        try (MongoCursor<Document> cursor = collectionMessages.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (object.has("time") && object.has("message") && object.has("from")) {
                    if (object.getString("time").equals(time) && object.getString("message").equals(message) && object.getString("from").equals(login)){
                        theQuery = new BasicDBObject();
                        theQuery.put("message", message);
                        collectionMessages.deleteOne(theQuery);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void updateUser(String fname, String lname, String login) {
        Bson filter = new Document("login", login);
        Bson newValue = new Document("fname", fname).append("lname", lname);

        Bson updateOperationDocument = new Document("$set", newValue);
        collectionUsers.updateMany(filter, updateOperationDocument);
    }

    public boolean findToken(String token) {
        try (MongoCursor<Document> cursor = collectionUsers.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (object.has("token")) {
                    if (object.getString("token").equals(token)) {
                        return true;
                    }
                }
            }
        }
        return false;
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

    public List<String> getLoggedUsers() {
        List<String> users = new ArrayList<>();
        try (MongoCursor<Document> cursor = collectionUsers.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());

                users.add(object.toString());
            }
        }
        return users;
    }

    public boolean matchToken(String login, String token) {
        try (MongoCursor<Document> cursor = collectionUsers.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (object.has("token")) {
                    if (object.getString("login").equals(login) && object.getString("token").equals(token)) {
                        return true;
                    }
                }
            }
        }
        return false;
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

    public List<String> getMessages(String login, String fromLogin) {
        List<String> messages = new ArrayList<>();
        try (MongoCursor<Document> cursor = collectionMessages.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if ((object.getString("to").equals(login) && object.getString("from").equals(fromLogin)) || (object.getString("from").equals(login) && object.getString("to").equals(fromLogin))){
                    messages.add(object.toString());
                }
            }
        }
        return messages;
    }

    public List<String> getMessages(String login) {
        List<String> messages = new ArrayList<>();
        try (MongoCursor<Document> cursor = collectionMessages.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (object.getString("to").equals(login)){
                    messages.add(object.toString());
                }
            }
        }
        return messages;
    }
}