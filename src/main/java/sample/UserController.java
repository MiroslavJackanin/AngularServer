package sample;

import com.google.gson.Gson;
import org.json.JSONException;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@CrossOrigin(origins = {"http://localhost:4200/"})
@RestController
public class UserController {
    List<User> list = new ArrayList<>();
    List<String> log = new ArrayList<>();
    List<String> messages = new ArrayList<>();

    public UserController() {
    }

    @RequestMapping(method = RequestMethod.POST, value = "/signup")
    public ResponseEntity<String> signup(@RequestBody String data) throws JSONException { // input data from body html page
        JSONObject jsonObject = new JSONObject(data);

        if (jsonObject.has("fname") && jsonObject.has("lname") && jsonObject.has("login") && jsonObject.has("password")) {
            if (existLogin(jsonObject.getString("login"))) {
                JSONObject result = new JSONObject();
                result.put("error", "Login already exists. Please Change Login ");
                return ResponseEntity.status(409).contentType(MediaType.APPLICATION_JSON).body(result.toString());
            }

            String password = jsonObject.getString("password");
            if (password.isEmpty()) {
                JSONObject result = new JSONObject();

                result.put("message", "Password is a mandatory field");
                return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(result.toString());
            }

            String hashPass = hash(jsonObject.getString("password"));
            User user = new User(jsonObject.getString("fname"), jsonObject.getString("lname"), jsonObject.getString("login"), hashPass);
            list.add(user);

            JSONObject res = new JSONObject();
            res.put("fname", jsonObject.getString("fname"));
            res.put("lname", jsonObject.getString("lname"));
            res.put("login", jsonObject.getString("login"));
            res.put("password", hashPass);
            res.put("token", "");

            Database db = new Database();
            db.insertUser(res);

            JSONObject messageFromServer = new JSONObject();
            messageFromServer.put("message", "Account successfully created");
            return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(messageFromServer.toString());
        } else {
            JSONObject res = new JSONObject();
            res.put("error", "Invalid body request");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "/login")
    public ResponseEntity<String> login(@RequestBody String credential) throws JSONException {
        JSONObject jsonObject = new JSONObject(credential);
        String time = getTime();

        if (jsonObject.has("login") && jsonObject.has("password")) {
            JSONObject result = new JSONObject();
            JSONObject logHistory = new JSONObject();

            if (jsonObject.getString("password").isEmpty() || jsonObject.getString("login").isEmpty()) {
                result.put("error", "Password and login are mandatory fields");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(result.toString());
            }

            if (existLogin(jsonObject.getString("login")) && checkPassword(jsonObject.getString("login"), jsonObject.getString("password"))) {
                User loggedUser = getUser(jsonObject.getString("login")); // create  user from login

                if (loggedUser.getFname() == null) {
                    return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{empty fname }");
                }

                logHistory.put("type", "login");
                logHistory.put("login", loggedUser.getLogin());
                logHistory.put("datetime", time);
                log.add(logHistory.toString());

                String token = generateNewToken();
                result.put("token", token);
                loggedUser.setToken(token);

                Database database = new Database();
                database.saveToken(jsonObject.getString("login"), token);
                database.saveLoginHistory(logHistory);

                return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(token);
            } else {
                result.put("error", "Invalid login or password");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(result.toString());
            }
        } else {
            JSONObject res = new JSONObject();
            res.put("error", "Invalid body request");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "/logout")
    public ResponseEntity<String> logout(@RequestBody String data, @RequestHeader(name = "Authorization") String token) throws JSONException {

        JSONObject objectInput = new JSONObject(data);
        JSONObject result = new JSONObject();
        String login = objectInput.getString("login");
        User user = getUser(login);

        if (user.getFname() == null) {
            result.put("error", "Login do not exist in our database");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(result.toString());
        }

        Database database = new Database();
        if (user.getFname() != null && database.existLogin(objectInput.getString("login"))) {
            if (database.existToken(token, user.getLogin())) {
                database.deleteToken(objectInput.getString("login"));

                user.setToken(null);
                return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body("{Successfully logout}");
            }
        }
        result.put("error", "Incorrect login or token");
        return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(result.toString());
    }

    @RequestMapping(method = RequestMethod.POST, value = "/changePassword")
    public ResponseEntity<String> changePassword(@RequestBody String data, @RequestHeader(name = "Authorization") String token) throws JSONException {
        JSONObject inputJson = new JSONObject(data);
        JSONObject resultJson = new JSONObject();

        User user = getUser(inputJson.getString("login"));

        if (user.getFname() == null) {
            resultJson.put("error", "Incorrect login");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(resultJson.toString());
        }
        if (inputJson.has("login") && inputJson.has("oldpassword") && inputJson.has("newpassword")) {

            if (user.getLogin().equals(inputJson.getString("login")) && BCrypt.checkpw(inputJson.getString("oldpassword"), user.getPassword())) {

                Database database = new Database();
                if (database.existToken(token, user.getLogin())) {
                    String hashPass = hash(inputJson.getString("newpassword"));
                    database.updatePassword(inputJson.getString("login"), hashPass);
                    user.setPassword(inputJson.getString("newpassword"));

                    return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body("{\"message\": \"Successfully changed password \"}");
                }
            }
            resultJson.put("error", "Wrong password or token");
        } else {
            resultJson.put("error", "Wrong input");
        }
        return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(resultJson.toString());
    }

    @RequestMapping(method = RequestMethod.POST, value = "/log")
    public ResponseEntity<String> log(@RequestBody String data, @RequestHeader(name = "Authorization") String token) throws JSONException {
        JSONObject obj = new JSONObject(data);
        JSONObject res = new JSONObject();

        if (obj.getString("login").isEmpty()) {
            res.put("error", "empty login ");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }

        User userObject = getUser(obj.getString("login"));

        if (userObject == null) {
            res.put("error", "incorrect login ");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
        if (obj.has("login")) {
            if (existLogin(obj.getString("login"))) {
                Database database = new Database();
                if (database.existToken(token, userObject.getLogin())) {

                    List<String> userlog = database.getLoginHistory(obj.getString("login"));
                    if (userlog != null) {
                        return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(userlog.toString());
                    } else {
                        res.put("message", "empty database with your records ");
                        return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(res.toString());
                    }
                } else {
                    res.put("error", "invalid token    ");
                    return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
                }
            } else {
                res.put("error", "login dos not exist in our database or array list   ");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }
        } else {
            res.put("error", "empty login name");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
    }

    @PostMapping(value = "/user")
    public ResponseEntity<String> getLoggedUser(@RequestBody String data, @RequestHeader(name = "token") String token) {
        org.json.JSONObject res = new org.json.JSONObject();
        org.json.JSONObject dat = new org.json.JSONObject(data);

        if (findToken(token)) {
            Database db = new Database();
            res = db.getLoggedUser(dat.getString("login"));
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
        res.put("error", "token not defined");
        return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
    }

    @RequestMapping("/users")
    public ResponseEntity<String> getUsers(@RequestHeader(name = "Authorization") String token, @RequestBody String data) throws JSONException {
        JSONObject inputData = new JSONObject(data);
        String From = inputData.getString("login");

        User userObject = getUser(From);
        if (token == null) {
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"Bad request 123\"}");
        }
        Database database = new Database();
        if (existLogin(From) && database.existToken(token, userObject.getLogin())) {
            List<String> users = database.getUsers();
            String jsonUsers = new Gson().toJson(users);
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(jsonUsers);
        } else
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"Invalid token\"}");
    }

    @PostMapping(value = "/loggedUsers")
    public ResponseEntity<String> getLoggedUsers(@RequestHeader(name = "Authorization") String token) {
        JSONObject res = new JSONObject();

        if (findToken(token)) {
            Database db = new Database();
            List<String> users;

            users = db.getLoggedUsers();
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(users.toString());
        } else {
            res.put("error", "token not found");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
    }

    @PostMapping(value = "/message/new")
    public ResponseEntity<String> newMessage(@RequestBody String data, @RequestHeader(name = "Authorization") String token) {
        org.json.JSONObject jsonObject = new org.json.JSONObject(data);
        JSONObject res = new JSONObject();

        String login = jsonObject.getString("from");

        if (login == null || !findToken(token)) {
            res.put("error", "invalid token or login");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
        if (matchToken(jsonObject.getString("from"), token) && findLogin(jsonObject.getString("from")) && findLogin(jsonObject.getString("to")) && jsonObject.has("message")) {
            String time = getTime();

            res.put("from", jsonObject.getString("from"));
            res.put("message", jsonObject.getString("message"));
            res.put("to", jsonObject.getString("to"));
            res.put("time", time);
            messages.add(res.toString());

            org.json.JSONObject dbMessage = new org.json.JSONObject();
            dbMessage.put("from", jsonObject.getString("from"));
            dbMessage.put("message", jsonObject.getString("message"));
            dbMessage.put("to", jsonObject.getString("to"));
            dbMessage.put("time", time);

            Database db = new Database();
            db.insertMessage(dbMessage);

            return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        } else {
            res.put("error", "wrong input data");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
    }

    @PostMapping(value = "/messages")
    public ResponseEntity<String> getMessages(@RequestBody String data, @RequestHeader(name = "Authorization") String token) {
        org.json.JSONObject jsonObject = new org.json.JSONObject(data);
        JSONObject res = new JSONObject();

        String login;
        String fromLogin = null;

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("Content-Type", "application/json; charset=UTF-8");

        if (jsonObject.has("login")) {
            if (findLogin(jsonObject.getString("login")) && findToken(token)) {
                login = jsonObject.getString("login");
            } else {
                res.put("error", "login or token not found");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }
            if (jsonObject.has("from")) {
                fromLogin = jsonObject.getString("from");
            }
        } else {
            res.put("error", "invalid input data");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }

        List<String> messages;
        Database db = new Database();

        if (fromLogin != null && matchToken(login, token)) {
            messages = db.getMessages(login, fromLogin);
            return new ResponseEntity<>(messages.toString(), responseHeaders, HttpStatus.CREATED);
        } else if (fromLogin == null) {
            messages = db.getMessages(login);
            return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(messages.toString());
        } else {
            res.put("error", "not authorised");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
    }

    @PostMapping(value = "/deleteMsg")
    public ResponseEntity<String> deleteMessage(@RequestBody String data, @RequestHeader(name = "Authorization") String token) {
        org.json.JSONObject jsonObject = new org.json.JSONObject(data);
        JSONObject res = new JSONObject();

        String login;
        String message;
        String time;

        if (jsonObject.has("login")) {
            if (findLogin(jsonObject.getString("login")) && findToken(token)) {
                login = jsonObject.getString("login");
            } else {
                res.put("error", "login or token not found");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }
            if (jsonObject.has("msg")) {
                message = jsonObject.getString("msg");
            } else {
                res.put("error", "message not found");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }
            if (jsonObject.has("time")) {
                time = jsonObject.getString("time");
            } else {
                res.put("error", "time not found");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }
        } else {
            res.put("error", "invalid input data");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }

        Database db = new Database();

        if (message != null && matchToken(login, token)) {
            if (db.deleteMessage(time, message, login)) {
                res.put("message", "delete message successful");
                return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }
        }
        res.put("message", "delete message unsuccessful");
        return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
    }


    @RequestMapping(value = "/playground")
    public ResponseEntity<String> playground() throws JSONException {
        System.out.println("messages without fname ");
        Database database = new Database();
        List<String> messagesList = database.getAllMessages();
        database.closeDatabase();
        return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(messagesList.toString());
    }

    @PostMapping(value = "/deleteAccount")
    public ResponseEntity<String> deleteAccount(@RequestBody String data, @RequestHeader(name = "Authorization") String token) throws JSONException {

        JSONObject objectInput = new JSONObject(data);
        JSONObject result = new JSONObject();
        String login = objectInput.getString("login");

        User user = getUser(login);

        if (user == null) {
            result.put("error", "Login does not exist in our database");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(result.toString());
        }

        Database database = new Database();

        if (user.getFname() != null && database.existLogin(objectInput.getString("login"))) {
            if (database.existToken(token, user.getLogin())) {
                if (checkPassword(objectInput.getString("login"), objectInput.getString("password"))) {
                    database.deleteUser(objectInput.getString("login"), token);
                    result.put("message", "Account delete successfully");

                    return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(result.toString());
                }

                result.put("error", "Incorrect password idiot try again");
                return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(result.toString());
            }
            result.put("error", "Bad token");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(result.toString());

        }
        result.put("error", "Incorrect login or token ");
        return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(result.toString());

    }

    @RequestMapping(method = RequestMethod.PATCH, value = "/update") // here
    public ResponseEntity<String> updateLogin(@RequestBody String data, @RequestHeader(name = "Authorization") String token) throws JSONException {

        JSONObject bodyData = new JSONObject(data);
        JSONObject result = new JSONObject();
        User user = getUser(bodyData.getString("login"));

        String login = bodyData.getString("login");
        Database database = new Database();

        if (user == null || !database.existToken(token, user.getLogin())) {
            result.put("error", "Incorrect login or invalid TOKEN ");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(result.toString());
        } else {
            if (existLogin(login)) {
                if (bodyData.has("fname") && !bodyData.has("lname")) {
                    user.setFname(bodyData.getString("fname"));
                    result.put("message", "Fname successfully changed");

                } else if (!bodyData.has("fname") && bodyData.has("lname")) {
                    user.setLname(bodyData.getString("lname"));
                    result.put("message", "Lname successfully changed");

                } else if (bodyData.has("fname") && bodyData.has("lname")) {
                    result.put("message", "Fname and L name successfully changed");

                    database.updateUser(bodyData.getString("fname"), bodyData.getString("lname"), bodyData.getString("login"));
                    database.closeDatabase();

                } else {
                    result.put("error", "Body input fname and lname are empty this values are required  ");
                    return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(result.toString());
                }
                return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(result.toString());
            } else {
                result.put("error", "Login do not exist in our database or list ");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(result.toString());
            }
        }
    }


    private boolean checkPassword(String login, String password) throws JSONException {
        User user = getUser(login);
        if (user != null) {
            return BCrypt.checkpw(password, user.getPassword());
        }
        return false;
    }

    private static String generateNewToken() {
        Random rand = new Random();
        long longToken = Math.abs(rand.nextLong());

        return Long.toString(longToken, 16);
    }

    private User getUser(String login) throws JSONException {
        Database database = new Database();
        JSONObject userJsonObject = database.getUser(login);

        if (userJsonObject != null) {
            return new User(userJsonObject.getString("fname"), userJsonObject.getString("lname"), userJsonObject.getString("login"), userJsonObject.getString("password"));
        } else {
            return null;
        }
    }

    private String getTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM HH:mm");
        LocalDateTime localTime = LocalDateTime.now();
        return dtf.format(localTime);
    }

    private boolean existLogin(String login) throws JSONException {
        Database database = new Database();
        return database.existLogin(login);
    }

    private String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(8));
    }

    private boolean findToken(String token) {
        Database db = new Database();
        return (db.findToken(token));
    }

    private boolean matchToken(String login, String token) {
        Database db = new Database();
        return db.matchToken(login, token);
    }

    private boolean findLogin(String login) {
        Database db = new Database();
        return (db.findLogin(login));
    }
}