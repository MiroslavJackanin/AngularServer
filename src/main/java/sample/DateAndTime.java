package sample;

import com.google.gson.Gson;
import net.minidev.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
public class DateAndTime {

    List<User> userList = new ArrayList<>();
    List<String> logList = new ArrayList<>();
    List<String> messages = new ArrayList<>();

    public DateAndTime(){
        userList.add(new User("Roman", "Maly", "Romaly", "hesllo"));
    }

    @RequestMapping("/time")
    public ResponseEntity<String> getTime(@RequestParam(value="token") String token) {

        if(token==null){
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{\"error\",\"Bad request\"}");
        }
        if(findToken(token)){
            JSONObject ris = new JSONObject();
            SimpleDateFormat sdfDate = new SimpleDateFormat("HH:mm:ss");
            Date now = new Date();
            String strTime = sdfDate.format(now);
            ris.put("time",strTime);
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(ris.toString());
        }
        else return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"Invalid token\"}");
    }

    @RequestMapping("/time/hour")
    public ResponseEntity<String> getHour(){
        SimpleDateFormat sdfDate = new SimpleDateFormat("HH");
        Date now = new Date();
        String strHour = sdfDate.format(now);
        return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body("{\"hour\":"+strHour+"}");
    }

    @RequestMapping("/primenumber/{number}")
    public ResponseEntity<String> checkPrimeNumber(@PathVariable String number) {
        try {
            int value = Integer.parseInt(number);
            boolean isPrimeNumber=true;
            if(value>1) {

                for (int j = 2; j <= Math.sqrt(value); j++)
                    if (value % j == 0) {
                        isPrimeNumber = false;
                        break;
                    }
            }
            else
                isPrimeNumber=false;

            JSONObject res = new JSONObject();
            res.put("number",value);
            res.put("primenumber",isPrimeNumber);

            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }catch(NumberFormatException e) {
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"Param must be integer\"}");
        }
    }

    @RequestMapping("/hello")
    public String getHello(){
        return "Hello, How are you ?";
    }

    @RequestMapping("/hello/{name}")
    public String getHelloWithName(@PathVariable String name){
        return "Hello "+name+". How are you? ";
    }

    @RequestMapping("/hi")
    public String getHi(@RequestParam(value="name") String name, @RequestParam(value="age") String age){
        return "Hello. How are you? Your name is "+name+" and you are "+age;
    }

    @PostMapping(value="/login")
    public ResponseEntity<String> login(@RequestBody String data){
        System.out.println(data);
        Gson gson = new Gson();
        User tempUser = gson.fromJson(data, User.class);
        JSONObject res = new JSONObject();

        if (tempUser.getLogin()!=null && tempUser.getPassword()!=null){
            if (matchLogin(tempUser.getLogin(), tempUser.getPassword())){
                res.put("error", "wrong password or login");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }
            String token = Long.toString(Math.abs(new SecureRandom().nextLong()), 16);
            res.put("firstName", tempUser.getFirstName());
            res.put("lastName", tempUser.getLastName());
            res.put("login", tempUser.getLogin());
            res.put("token", token);
        }
        logLogin(tempUser);
        return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(res.toString());
    }

    private void logLogin(User tempUser) {
        JSONObject log = new JSONObject();
        log.put("type", "login");
        log.put("login", tempUser.getLogin());
        log.put("datetime", getTime(tempUser.getToken()));
        logList.add(log.toString());
    }

    @PostMapping(value="/logout")
    public ResponseEntity<String> logout(@RequestBody String data, @RequestHeader(name = "Authorization") String token){
        System.out.println(data);

        Gson gson = new Gson();
        User user = gson.fromJson(data, User.class);
        System.out.println(user.getLogin());

        if (findToken(token)){
            for(User users : userList)
                if(users.getLogin().equals(user.getLogin()))
                    users.setToken(null);

            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body("{}");
        }

        JSONObject res = new JSONObject();
        res.put("error","Incorrect login or token");
        logLogout(user);
        return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
    }

    private void logLogout(User user) {
        JSONObject log = new JSONObject();
        log.put("type", "logout");
        log.put("login", user.getLogin());
        log.put("datetime", getTime(user.getToken()));
        logList.add(log.toString());
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
            String passwordHash = BCrypt.hashpw(tempUser.getPassword(), BCrypt.gensalt());
            User user = new User(tempUser.getFirstName(), tempUser.getLastName(), tempUser.getLogin(), passwordHash);
            userList.add(user);

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
    public ResponseEntity<List<JSONObject>> getLoggedUsers(@PathVariable String token){
        List<JSONObject> list = new ArrayList<>();

        if (findToken(token)){
            for (User user : userList) {
                JSONObject res = new JSONObject();
                res.put("firstName", user.getFirstName());
                res.put("lastName", user.getLastName());
                res.put("login", user.getLogin());
                list.add(res);
            }
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(list);
        }
        JSONObject res = new JSONObject();
        res.put("error", "no users logged in");
        list.add(res);
        return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(list);
    }

    @GetMapping(value = "/users/{userLogin}?token={token}")
    public ResponseEntity<String> getLoggedUser(@PathVariable String userLogin, String token){
        JSONObject res = new JSONObject();

        if (token != null){
            for (User user : userList) {
                if (user.getLogin().equals(userLogin) && user.getToken().equals(token)) {
                    res.put("firstName", user.getFirstName());
                    res.put("lastName", user.getLastName());
                    res.put("login", user.getLogin());
                    return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(res.toString());
                }
            }
        }
        res.put("error", "token not defined");
        return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
    }

    @PostMapping(value = "/changepassword")
    public ResponseEntity<String> changePassword(@RequestBody String data, @RequestHeader String token){
        Gson gson = new Gson();
        org.json.JSONObject user = new org.json.JSONObject(data);
        JSONObject res = new JSONObject();

        if (user.getString("login") != null && user.getString("oldpassword") != null && user.getString("newpassword") != null){
            if (!matchToken(user.getString("login"), token)){
                res.put("error", "no login with such token");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }
            if (matchLogin(user.getString("login"), user.getString("oldpassword"))){
                res.put("error", "wrong password or login");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }

            for (User users : userList) {
                if (users.getLogin().equals(user.getString("login"))) {
                    users.setPassword(user.getString("newpassword"));
                    res.put(users.getLogin(), "password changed");
                    return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(res.toString());
                }
            }
        }
        res.put("error", "missing body attributes");
        return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
    }

    @GetMapping(value = "/log")
    public ResponseEntity<String> getLogList(@RequestHeader String token){
        JSONObject res = new JSONObject();
        List<String> userLog = new ArrayList<>();
        if (!findToken(token)){
            res.put("error", "invalid token");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }

        String login = "";
        for(User user : userList){
            if(user.getToken().equals(token) && user.getToken()!=null){
                login = user.getLogin();
            }
        }

        org.json.JSONObject logObj = null;
        for (String log:logList) {
            logObj = new org.json.JSONObject(log);
            if (logObj.getString("login").equals(login)){
                userLog.add(log);
            }
        }
        return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(userLog.toString());
    }

    @RequestMapping(method = RequestMethod.POST, value = "/message/new")
    public ResponseEntity<String> newMessage(@RequestBody String data, @RequestHeader(name = "Authorization") String token){
        org.json.JSONObject jsonObject = new org.json.JSONObject(data);
        JSONObject res = new JSONObject();

        String login = jsonObject.getString("from");

        if (login == null  || !findToken(token) ) {
            res.put("error", "invalid token or login");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }

        if (findLogin(jsonObject.getString("from")) && findLogin(jsonObject.getString("to")) && jsonObject.has("message")) {
            res.put("from", jsonObject.getString("from"));   // sender
            res.put("message", jsonObject.getString("message"));   // message
            res.put("to", jsonObject.getString("to"));   // acceptor

            JSONArray jsonArray = new JSONArray();
            jsonArray.put(res);

            messages.add(res.toString());
            return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        } else {
            res.put("error", "wrong input data");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/messages")
    public ResponseEntity<String> getMessages(@RequestBody String data, @RequestHeader(name = "Authorization") String token) throws JSONException {

        org.json.JSONObject jsonObject = new org.json.JSONObject(data);
        JSONObject res = new JSONObject();

        String login = jsonObject.getString("login");
        if (login == null  || !findToken(token) ) {
            res.put("error", "invalid token or login");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }

        if (jsonObject.has("login") && findLogin(jsonObject.getString("login"))) {
            res.put("from", jsonObject.getString("login"));
            for(int i = 0; i < messages.size(); i++) {
                res.put("message" + i, messages.get(i));
            }
            return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        } else {
            res.put("error", "missing or wrong login");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
    }

    private boolean findToken(String token) {
        for(User user : userList){
            if(user.getToken().equals(token) && user.getToken()!=null)
                return true;
        }
        return false;
    }

    private boolean matchLogin(String login, String password) {
        for (User user : userList) {
            if (user.getLogin().equals(login) && user.getPassword().equals(password)) {
                return false;
            }
        }
        return true;
    }

    private boolean findLogin(String login) {
        for(User user : userList){
            if(user.getLogin().equals(login))
                return true;
        }
        return false;
    }

    private boolean matchToken(String login, String token){
        for (User user : userList) {
            if (user.getLogin().equals(login) && user.getToken().equals(token)) {
                return true;
            }
        }
        return false;
    }
}
