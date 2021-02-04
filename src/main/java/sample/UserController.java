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
        list.add(new User("peter", "sagan", "sagan", "$2a$08$pwyuZ84u7Qp2P.vFHS5vZ.ei2brZdqWY3NiUycbhY0tJgYCnpFaEG"));
        list.add(new User("veronika", "veronika", "veronika", "$2a$08$pwyuZ84u7Qp2P.vFHS5vZ.ei2brZdqWY3NiUycbhY0tJgYCnpFaEG"));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/signup")
    public ResponseEntity<String> signup(@RequestBody String data) throws JSONException { // input data from body html page
        System.out.println("input data are under");
        System.out.println(data);

        JSONObject jsonObject = new JSONObject(data);

        // CHECK WE HAVE ALL DATA
        if (jsonObject.has("fname") && jsonObject.has("lname") && jsonObject.has("login") && jsonObject.has("password")) {

            System.out.println("I have alll name "); // control  show data in console
            System.out.println(" I have all ");

            // CHECK  EXIST LOGIN
            if (existLogin(jsonObject.getString("login"))) {  // take login into function exist Login   return true or false
                JSONObject result = new JSONObject();          // create json object
                result.put("error", "Login already exists. Please Change Login ");         // put error message
                return ResponseEntity.status(409).contentType(MediaType.APPLICATION_JSON).body(result.toString()); // to string okey
                // note what means contentType media type check the video
            }

            //CHECK  PASSWORD IS EMPTY
            String password = jsonObject.getString("password");

            if (password.isEmpty()) {
                JSONObject result = new JSONObject();

                result.put("message", "Password is a mandatory field");
                return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(result.toString());
            }

            String hashPass = hash(jsonObject.getString("password")); // create hash

            User user = new User(jsonObject.getString("fname"), jsonObject.getString("lname"), jsonObject.getString("login"), hashPass);
            list.add(user);

            JSONObject res = new JSONObject(); // create json
            res.put("fname", jsonObject.getString("fname"));  // put message
            res.put("lname", jsonObject.getString("lname"));
            res.put("login", jsonObject.getString("login"));
            res.put("password", hashPass);
            res.put("token", "");

            Database db = new Database();  // create database
            db.insertUser(res);             // send JSON data

            System.out.println("Successfully created new account and save in database ");

            JSONObject messageFromServer = new JSONObject(); // create json
            messageFromServer.put("message", "Account successfully created");

            return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(messageFromServer.toString()); // to string json data
            // return "I have all name okey ";
        } else {
            JSONObject res = new JSONObject();
            res.put("error", "Invalid body request");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            // return "I dont have";
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "/login")
    public ResponseEntity<String> login(@RequestBody String credential) throws JSONException {
        JSONObject jsonObject = new JSONObject(credential);  // why

        // time actually
        String time = getTime();

        //CHECK WE HAVE LOGIN AND PASSWORD
        if (jsonObject.has("login") && jsonObject.has("password")) {

            JSONObject result = new JSONObject(); // CREATE NEW JSON OBJECT
            JSONObject logHistory = new JSONObject(); // CREATE NEW JSON OBJECT

            //check password and login that are empty
            if (jsonObject.getString("password").isEmpty() || jsonObject.getString("login").isEmpty()) {
                result.put("error", "Password and login are mandatory fields");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(result.toString());
            }

            String hashInputPassword = hash(jsonObject.getString("password")); // create hash
            System.out.println("your hash input password   " + hashInputPassword);

            //check the existing the login and check password than is correct
            if (existLogin(jsonObject.getString("login")) && checkPassword(jsonObject.getString("login"), jsonObject.getString("password"))) {
                User loggedUser = getUser(jsonObject.getString("login")); // create  user from login

                if (loggedUser.getFname() == null) {
                    // tento riadok by sa nemal nikdy vykonat, osetrene kvoli jave
                    return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{empty fname }");
                }

                System.out.println("---                                         ----");
                System.out.println("loggedUSer class name and fname " + loggedUser.getFname());
                System.out.println("loggedUSer class name and lname " + loggedUser.getLname());
                System.out.println("---                                         ----");

                // put data into json object
                logHistory.put("type", "login");
                logHistory.put("login", loggedUser.getLogin());
                logHistory.put("datetime", time);

                System.out.println("history" + logHistory);
                // Generate new token
                log.add(logHistory.toString()); //add into the list time

                String token = generateNewToken();

                System.out.println("generate token is  " + token);
                System.out.println("time  " + time);

                result.put("token", token);
                loggedUser.setToken(token);  // set token

                // save token into database
                Database database = new Database();
                database.saveToken(jsonObject.getString("login"), token);

                //save login history into database
                database.saveLoginHistory(logHistory);

                System.out.println("show me the login name " + jsonObject.getString("login"));

                /// also sent the token

                System.out.println("-- ..................TOKEN..................");
                System.out.println("token is " + loggedUser.getToken());
                System.out.println("-- ..................TOKEN..................");

                //   Strinng JSondatatime  = {"type":"logout","login":"martin5","datetime":"04052020 13:58:04"}
                // better tocreate JSON object

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
            System.out.println("user is null");

            result.put("error", "Login do not exist in our database");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(result.toString());

        }
        System.out.println("===============================================================");
        System.out.println("==                                                            ==");
        System.out.println("user is " + user);

        System.out.println("validToken is " + validToken(token, user.getToken()));
        System.out.println("token " + token);
        System.out.println("userToken " + user.getToken());

        System.out.println("===============================================================");
        System.out.println("==                                                            ==");

        // create database
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
        JSONObject inputJson = new JSONObject(data); // json from input data body
        JSONObject resultJson = new JSONObject();//m result JSON

        User user = getUser(inputJson.getString("login")); // return object user according to login name

        // check user we have in database t
        if (user.getFname() == null) {
            resultJson.put("error", "Incorrect login");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(resultJson.toString());
        }
        // check we have data like login old password and new password
        if (inputJson.has("login") && inputJson.has("oldpassword") && inputJson.has("newpassword")) {
            System.out.println(inputJson.getString("oldpassword"));

            if (user.getLogin().equals(inputJson.getString("login")) &&
                    BCrypt.checkpw(inputJson.getString("oldpassword"), user.getPassword())) {

                Database database = new Database();
                if (database.existToken(token, user.getLogin())) {
                    //  System.out.println("change  passwrod to " + inputJson.getString("newpassword"));
                    String hashPass = hash(inputJson.getString("newpassword")); // create hash

                    database.updatePassword(inputJson.getString("login"), hashPass);

                    // better to add return 200 with body message success
                    user.setPassword(inputJson.getString("newpassword"));
                    ///  update into database  create

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

        if (userObject == null) { // check we have user and check the token
            res.put("error", "incorrect login ");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
        if (obj.has("login")) {
            //existLogin(obj.getString("login")) && existLogin(obj.getString("acceptor")r
            if (existLogin(obj.getString("login"))) {
                // res.put("message", "everythink is okey ");
                Database database = new Database();
                if (database.existToken(token, userObject.getLogin())) {
                    //JSONObject loginHistory = database.getLoginHistory(obj.getString("login"));

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

    /// Log with parameter and return values according to input value
    // todo  dokoncit
    //pridat moznost volitelneho parametra localhost:8080/log?type=logout

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

        System.out.println("This is From  " + From);

        if (inputData.getString("login") != null) {
            System.out.println("We have From " + From);
        }

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
    public ResponseEntity<String> getLoggedUsers(@RequestHeader(name = "Authorization") String token){
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

    /////////////////////// DELETE ACCOUNT   vytvorit DELETE request localhost:8080/delete/login
// todo potrebne dokncit
    @PostMapping(value = "/deleteAccount")
    public ResponseEntity<String> deleteAccount(@RequestBody String data, @RequestHeader(name = "Authorization") String token) throws JSONException {

        // input data RequestBody  login, password
        // Authorization token


        JSONObject objectInput = new JSONObject(data);
        JSONObject result = new JSONObject();
        String login = objectInput.getString("login");

        User user = getUser(login);

        if (user == null) {
            System.out.println("user is null");

            result.put("error", "Login does not exist in our database");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(result.toString());

        }

        // create database
        Database database = new Database();

        if (user.getFname() != null && database.existLogin(objectInput.getString("login"))) {
            if (database.existToken(token, user.getLogin())) {
                // database.deleteToken(objectInput.getString("login"));

                //  user.setToken(null);

                // check the password
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

    /*vytvorit PATCH request localhost:8080/update/login
pricom login bude nase meno, header ma token.
v Body bude udaje co chceme zmenit, a to moze byt len fname alebo lname (prip obe)*/

    @RequestMapping(method = RequestMethod.PATCH, value = "/update") // here
    public ResponseEntity<String> updateLogin(@RequestBody String data, @RequestHeader(name = "Authorization") String token) throws JSONException {

        JSONObject bodyData = new JSONObject(data);
        JSONObject result = new JSONObject();
        User user = getUser(bodyData.getString("login"));

        String login = bodyData.getString("login");
        // pricom login bude nase meno, header ma token.
        //v Body bude udaje co chceme zmenit, a to moze byt len fname alebo lname (prip obe)

        // I have fname
        Database database = new Database();

        if (user == null || !database.existToken(token, user.getLogin())) { // check we have user and check the token
            result.put("error", "Incorrect login or invalid TOKEN ");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(result.toString());
        } else {
            // check the login exist
            if (existLogin(login)) {
                //update
                // I have fname
                if (bodyData.has("fname") && !bodyData.has("lname")) {
                    user.setFname(bodyData.getString("fname"));
                    result.put("message", "Fname successfully changed");

                    //  return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(result.toString());
                    // I have only the lname
                } else if (!bodyData.has("fname") && bodyData.has("lname")) {
                    user.setLname(bodyData.getString("lname"));
                    result.put("message", "Lname successfully changed");
                    //return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(result.toString());

                    // I have fname and also lname
                } else if (bodyData.has("fname") && bodyData.has("lname")) {// I have fname and login
                    result.put("message", "Fname and L name successfully changed");

                    // input parameter  fname lname
                    System.out.println("body data fname  " + bodyData.getString("fname") + "body data getString lname " + bodyData.getString("lname"));

                    database.updateUser(bodyData.getString("fname"), bodyData.getString("lname"), bodyData.getString("login"));
                    database.closeDatabase();

                    //return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(result.toString());

                } else { // I do not have fname and lname what is problem
                    //change nothing because we don have values
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
            System.out.println("---                                             ----");
            System.out.println("password function check password is " + user.getPassword());
            System.out.println("---                                             ----");

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
            System.out.println("userJsonobject " + userJsonObject);
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

    private boolean validToken(String token, String user) {
        return token.equals(user);
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