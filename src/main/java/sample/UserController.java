package sample;

import com.google.gson.Gson;
import net.minidev.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

@RestController
public class UserController {

    List<User> userList = new ArrayList<>();
    List<String> logList = new ArrayList<>();
    List<String> messages = new ArrayList<>();

    public UserController(){
        //userList.add(new User("User", "Name", "UsName", "pass"));
    }

    @RequestMapping("/time")
    public ResponseEntity<String> getTime(@RequestParam(value="token") String token) {
        if(token==null){
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{\"error\",\"Bad request\"}");
        }
        if(findToken(token)){
            JSONObject res = new JSONObject();
            SimpleDateFormat sdfDate = new SimpleDateFormat("HH:mm:ss");
            Date now = new Date();
            String strTime = sdfDate.format(now);
            res.put("time",strTime);
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
        else return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"Invalid token\"}");
    }

    @PostMapping(value="/login")
    public ResponseEntity<String> login(@RequestBody String data){
        System.out.println(data);
        Gson gson = new Gson();
        User tempUser = gson.fromJson(data, User.class);
        JSONObject res = new JSONObject();

        if (tempUser.getLogin()!=null && tempUser.getPassword()!=null){
            if (!matchLogin(tempUser.getLogin(), tempUser.getPassword())){
                System.out.println(tempUser.getLogin());
                System.out.println(tempUser.getPassword());
                res.put("error", "wrong password or login");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }
            String token = generateToken();
            Database db = new Database();
            db.login(tempUser.getLogin(), token);

            org.json.JSONObject user = db.getUser(tempUser.getLogin());
            res.put("firstName", user.getString("firstName"));
            res.put("lastName", user.getString("lastName"));
            res.put("login", tempUser.getLogin());
            res.put("token", token);
        }
        logLogin(tempUser);
        return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(res.toString());
    }

    @PostMapping(value="/logout")
    public ResponseEntity<String> logout(@RequestBody String data, @RequestHeader(name = "Authorization") String token){
        System.out.println(data);

        Gson gson = new Gson();
        User user = gson.fromJson(data, User.class);
        System.out.println(user.getLogin());

        if (findToken(token)){
            Database db = new Database();
            db.logout(user.getLogin(), token);

            logLogout(user);
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body("{}");
        }

        JSONObject res = new JSONObject();
        res.put("error","Incorrect login or token");
        return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
    }

    @PostMapping(value="/signup")
    public ResponseEntity<String> signUp(@RequestBody String data){
        System.out.println(data);
        Gson gson = new Gson();
        User tempUser = gson.fromJson(data, User.class);
        JSONObject res = new JSONObject();

        if (tempUser.getFirstName()!=null && tempUser.getLastName()!=null && tempUser.getLogin()!=null && tempUser.getPassword()!=null) {
            if(findLogin(tempUser.getLogin())){
                res.put("error","user already exists");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }
            if(tempUser.getPassword().isEmpty()){
                res.put("error","password is a mandatory field");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }
            String passwordHash = hash(tempUser.getPassword());
            User user = new User(tempUser.getFirstName(), tempUser.getLastName(), tempUser.getLogin(), passwordHash);
            userList.add(user);

            org.json.JSONObject dbUser = new org.json.JSONObject();
            dbUser.put("firstName", tempUser.getFirstName());
            dbUser.put("lastName", tempUser.getLastName());
            dbUser.put("login", tempUser.getLogin());
            dbUser.put("password", passwordHash);
            Database db = new Database();
            db.insertUser(dbUser);

            res.put("firstName",tempUser.getFirstName());
            res.put("lastName",tempUser.getLastName());
            res.put("login",tempUser.getLogin());
            return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
        else{
            res.put("error","invalid input");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
    }

    @GetMapping(value = "/users?token={token}")
    public ResponseEntity<List<org.json.JSONObject>> getLoggedUsers(@PathVariable(name = "token") String token){
        List<org.json.JSONObject> list = new ArrayList<>();

        if (findToken(token)){
            /*for (User user : userList) {
                JSONObject res = new JSONObject();
                res.put("firstName", user.getFirstName());
                res.put("lastName", user.getLastName());
                res.put("login", user.getLogin());
                list.add(res);
            }*/
            Database db = new Database();
            list=db.getLoggedUsers();
            /*System.out.println("lsit");
            for (org.json.JSONObject jsonObject : list) {
                System.out.println(jsonObject.toString());
            }*/
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(list);
        }
        org.json.JSONObject res = new org.json.JSONObject();
        res.put("error", "no users logged in");
        list.add(res);
        return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(list);
    }

    @GetMapping(value = "/?user={userLogin}&?token={token}")
    public ResponseEntity<String> getLoggedUser(@PathVariable(name = "userLogin") String userLogin, @PathVariable(name = "token") String token){
        org.json.JSONObject res = new org.json.JSONObject();

        if (findToken(token)){
            /*for (User user : userList) {
                if (user.getLogin().equals(userLogin) && user.getToken().equals(token)) {
                    res.put("firstName", user.getFirstName());
                    res.put("lastName", user.getLastName());
                    res.put("login", user.getLogin());
                }
            }*/
            Database db = new Database();
            res = db.getLoggedUser(userLogin);
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
        res.put("error", "token not defined");
        return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
    }

    @PostMapping(value = "/changepassword")
    public ResponseEntity<String> changePassword(@RequestBody String data, @RequestHeader(name = "Authorisation") String token){
        org.json.JSONObject user = new org.json.JSONObject(data);
        JSONObject res = new JSONObject();

        if (user.getString("login") != null && user.getString("oldpassword") != null && user.getString("newpassword") != null){
            if (!matchToken(user.getString("login"), token)){
                res.put("error", "no login with such token");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }
            if (!matchLogin(user.getString("login"), user.getString("oldpassword"))){
                res.put("error", "wrong password or login");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }
            String passHash = hash(user.getString("newpassword"));

            Database db = new Database();
            db.changePassword(user.getString("login"), passHash);
            res.put(user.getString("login"), "password changed");
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
        res.put("error", "missing body attributes");
        return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
    }

    @GetMapping(value = "/log?type={logType}")
    public ResponseEntity<String> getLogList(@RequestHeader(name = "Authorization") String token, @PathVariable(name = "logType") String logType){
        JSONObject res = new JSONObject();
        List<String> userLog;
        if (!findToken(token)){
            res.put("error", "invalid token");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }

        Database db = new Database();
        String login = db.getLogin(token);

        userLog = db.getLogs(login, logType);
        return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(userLog.toString());
    }

    @PostMapping(value = "/message/new")
    public ResponseEntity<String> newMessage(@RequestBody String data, @RequestHeader(name = "Authorization") String token){
        org.json.JSONObject jsonObject = new org.json.JSONObject(data);
        JSONObject res = new JSONObject();

        String login = jsonObject.getString("from");

        if (login == null  || !findToken(token) ) {
            res.put("error", "invalid token or login");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
        if (findLogin(jsonObject.getString("from")) && findLogin(jsonObject.getString("to")) && jsonObject.has("message")) {
            res.put("from", jsonObject.getString("from"));
            res.put("message", jsonObject.getString("message"));
            res.put("to", jsonObject.getString("to"));
            messages.add(res.toString());

            org.json.JSONObject dbMessage = new org.json.JSONObject();
            dbMessage.put("from", jsonObject.getString("from"));
            dbMessage.put("message", jsonObject.getString("message"));
            dbMessage.put("to", jsonObject.getString("to"));

            Database db = new Database();
            db.insertMessage(dbMessage);

            return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        } else {
            res.put("error", "wrong input data");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
    }

    @GetMapping(value = "/messages?from={fromLogin}")
    public ResponseEntity<String> getMessages(@RequestBody String data, @RequestHeader(name = "Authorization") String token, @PathVariable String fromLogin){

        org.json.JSONObject jsonObject = new org.json.JSONObject(data);
        JSONObject res = new JSONObject();

        String login = jsonObject.getString("login");
        if (login == null  || !findToken(token) ) {
            res.put("error", "invalid token or login");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }

        org.json.JSONObject message;
        if (jsonObject.has("login") && findLogin(jsonObject.getString("login"))) {
            res.put("from", jsonObject.getString("login"));
            for(int i = 0; i < messages.size(); i++) {
                message = new org.json.JSONObject(messages.get(i));
                if (message.getString("from").equals(fromLogin) && !fromLogin.equals("")){
                    res.put("message" + i, messages.get(i));
                }
            }
            return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        } else {
            res.put("error", "missing or wrong login");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
    }

    @DeleteMapping(value = "/delete/{login}")
    public ResponseEntity<String> deleteUser(@RequestHeader(name = "Authorisation") String token, @PathVariable String login){
        JSONObject res = new JSONObject();
        if (matchToken(login, token)){
            org.json.JSONObject jsonObject;
            for (int i=0; i<messages.size(); i++){
                jsonObject = new org.json.JSONObject(messages.get(i));
                if (jsonObject.getString("from").equals(login)){
                    messages.remove(messages.get(i));
                }
            }
            for (int i=0; i<userList.size();i++){
                if (userList.get(i).getLogin().equals(login)){
                    userList.remove(userList.get(i));
                }
            }

            Database db = new Database();
            db.deleteUser(login);

            res.put("status", "user removed");
            return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }else {
            res.put("error", "wrong login or token");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
    }

    @PatchMapping(value = "update/{login}")
    public ResponseEntity<String> updateLogin(@RequestBody String data, @RequestHeader String token, @PathVariable String login){
        org.json.JSONObject jsonObject = new org.json.JSONObject(data);
        JSONObject res = new JSONObject();

        if (!matchToken(login, token)){
            res.put("error", "wrong token or login");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }

        if (jsonObject.has("firstName")) {
            String name = "";
            for (User user : userList) {
                if (user.getLogin().equals(login))
                    name = user.getFirstName();
                    user.setFirstName(jsonObject.getString("firstName"));
            }
            Database db = new Database();
            db.updateFName(name, jsonObject.getString("firstName"));
        }
        if (jsonObject.has("lastName")) {
            String name = "";
            for (User user : userList) {
                if (user.getLogin().equals(login))
                    name = user.getLastName();
                    user.setLastName(jsonObject.getString("lastName"));
            }
            Database db = new Database();
            db.updateLName(name, jsonObject.getString("lastName"));
        }

        res.put("status", "data changed");
        return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(res.toString());
    }

    private String generateToken(){
        int size=25;
        Random rnd = new Random();
        StringBuilder generatedString= new StringBuilder();
        for(int i = 0;i<size;i++) {
            int type=rnd.nextInt(4);
            switch (type) {
                case 0:
                    generatedString.append((char) ((rnd.nextInt(26)) + 65));
                    break;
                case 1:
                    generatedString.append((char) ((rnd.nextInt(10)) + 48));
                    break;
                default:
                    generatedString.append((char) ((rnd.nextInt(26)) + 97));
            }
        }
        return generatedString.toString();
    }

    private boolean findToken(String token) {
        Database db = new Database();
        return (db.findToken(token));
    }

    private boolean matchLogin(String login, String password) {
        Database db = new Database();
        return db.matchLogin(login, password);
    }

    private boolean findLogin(String login) {
        Database db = new Database();
        return (db.findLogin(login));
    }

    private boolean matchToken(String login, String token){
        Database db = new Database();
        return db.matchToken(login, token);
    }

    public String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    private void logLogout(User user) {
        org.json.JSONObject log = new org.json.JSONObject();
        log.put("type", "logout");
        log.put("login", user.getLogin());
        log.put("datetime", getTime());
        logList.add(log.toString());

        Database db = new Database();
        db.logLogout(log);
    }

    private void logLogin(User tempUser) {
        org.json.JSONObject log = new org.json.JSONObject();
        log.put("type", "login");
        log.put("login", tempUser.getLogin());
        log.put("datetime", getTime());
        logList.add(log.toString());

        Database db = new Database();
        db.logLogin(log);
    }

    private String getTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("ddMMyy HH:mm:ss");
        LocalDateTime localTime = LocalDateTime.now();
        return dtf.format(localTime);

    }
}