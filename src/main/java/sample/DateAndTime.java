package sample;

import com.google.gson.Gson;
import net.minidev.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@RestController
public class DateAndTime {

    List<User> userList = new ArrayList<>();

    public DateAndTime(){
        userList.add(new User("Roman", "Maly", "Romaly", "hesllo"));
    }

    @RequestMapping("/time")
    public String getTime(){
        return "11:29:15";
    }
    @RequestMapping("/time/hour")
    public String getHour(){
        return "11";
    }
    @RequestMapping("/primenumber/{number}")
    public String checkPrimeNumber(@PathVariable int number){
        return number + "true/false";
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
            if (!matchLogin(tempUser.getLogin(), tempUser.getPassword())){
                res.put("error", "wrong password or login");
                return ResponseEntity.status(400).body(res.toString());
            }
            String token = Long.toString(Math.abs(new SecureRandom().nextLong()), 16);
            res.put("firstName", tempUser.getFirstName());
            res.put("lastName", tempUser.getLastName());
            res.put("login", tempUser.getLogin());
            res.put("token", token);
        }
        return ResponseEntity.status(201).body(res.toString());
    }

    @PostMapping(value="/logout")
    public ResponseEntity<String> logout(@RequestBody String data){
        System.out.println(data);

        Gson gson = new Gson();
        User user = gson.fromJson(data, User.class);
        System.out.println(user.getLogin());

        JSONObject res = new JSONObject();
        res.put("message","LogOut successful");
        res.put("login",user.getLogin());
        return ResponseEntity.status(200).body(res.toString());
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
                return ResponseEntity.status(400).body(res.toString());
            }
            if(tempUser.getPassword().isEmpty()){
                res.put("error","password is a mandatory field");
                return ResponseEntity.status(400).body(res.toString());
            }
            String passwordHash = BCrypt.hashpw(tempUser.getPassword(), BCrypt.gensalt());
            User user = new User(tempUser.getFirstName(), tempUser.getLastName(), tempUser.getLogin(), passwordHash);
            userList.add(user);

            res.put("firstName",tempUser.getFirstName());
            res.put("lastName",tempUser.getLastName());
            res.put("login",tempUser.getLogin());
            return ResponseEntity.status(201).body(res.toString());
        }
        else{
            res.put("error","invalid input");
            return ResponseEntity.status(400).body(res.toString());
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
            return ResponseEntity.status(200).body(list);
        }
        JSONObject res = new JSONObject();
        res.put("error", "no users logged in");
        list.add(res);
        return ResponseEntity.status(401).body(list);
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
                    return ResponseEntity.status(200).body(res.toString());
                }
            }
        }
        res.put("error", "token not defined");
        return ResponseEntity.status(401).body(res.toString());
    }

    private boolean findToken(String token) {
        for(User user : userList){
            if(user.getToken().equals(token))
                return true;
        }
        return false;
    }

    private boolean matchLogin(String login, String password) {
        for (User user : userList) {
            if (user.getLogin().equals(login) && user.getPassword().equals(password)) {
                return true;
            }
        }
        return false;
    }

    private boolean findLogin(String login) {
        for(User user : userList){
            if(user.getLogin().equals(login))
                return true;
        }
        return false;
    }
}
