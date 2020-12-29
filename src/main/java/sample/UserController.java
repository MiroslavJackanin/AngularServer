package sample;


import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@CrossOrigin(origins = {  "http://localhost:4200/" })
@RestController
public class UserController {
    List<User> list = new ArrayList<User>();
    List<String> log = new ArrayList<>();
    List<String> messages = new ArrayList<>();

    public UserController() {
        list.add(new User("peter", "sagan", "sagan", "$2a$08$pwyuZ84u7Qp2P.vFHS5vZ.ei2brZdqWY3NiUycbhY0tJgYCnpFaEG"));
        list.add(new User("veronika", "veronika", "veronika", "$2a$08$pwyuZ84u7Qp2P.vFHS5vZ.ei2brZdqWY3NiUycbhY0tJgYCnpFaEG"));

        /*
        list.add(new User("peter", "juliana", "juliana", "$2a$08$pwyuZ84u7Qp2P.vFHS5vZ.ei2brZdqWY3NiUycbhY0tJgYCnpFaEG"));
        list.add(new User("peter", "dog", "Dog", "$2a$08$pwyuZ84u7Qp2P.vFHS5vZ.ei2brZdqWY3NiUycbhY0tJgYCnpFaEG"));
*/

    }


    // check all users than someone has equals token
    public boolean validToken(String token , String user) {
        return token.equals(user);
    }

    /*
    *  Database database = new Database();
        JSONObject userJsonObject = database.getUser(login);
        System.out.println("userJsonobject " + userJsonObject);*/

    ///////////////////////////////////////////
    // SIGN UP

    // input user registration

    /*Input
     *{
"fname":"new user",
"lname":"second name",
"login":"user24",
"password":"password123123"
}
    *
    * */
    @RequestMapping(method = RequestMethod.POST, value = "/signup")
    public ResponseEntity<String> signup(@RequestBody String data) throws JSONException { // input data from body html page
//
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


        // todo create the hash bcrypt
        // todo uniq login  you have to check  ,, use array list and compare data
        //return null;
    }

    private boolean existLogin(String login) throws JSONException {
        Database database = new Database();
        return  database.existLogin(login);
        // return true or false

    }

    public String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(8));
    }


    ///////////////////////////////////////////
    /////   LOGIN

    //  login
    // password

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

                User loggedUser = getUser(jsonObject.getString("login")) ; // create  user from login


                if (loggedUser.getFname() == null) {
                    // tento riadok by sa nemal nikdy vykonat, osetrene kvoli jave
                    return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{empty fname }");
                }

                System.out.println("---                                         ----");
                System.out.println("loggedUSer class name and fname " + loggedUser.getFname() );
                System.out.println("loggedUSer class name and lname " + loggedUser.getLname() );
                System.out.println("---                                         ----");

/*
                result.put("fname", loggedUser.getFname());
                result.put("lname", loggedUser.getLname());
                result.put("login", loggedUser.getLogin());
*/


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
                database.saveToken(jsonObject.getString("login"),token);

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


    /////////////////////////////////////////////////////////// END LOGIN


    //////////////   LOGOUT

    // login and token
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

            if (database.existToken(token,user.getLogin())) {
                database.deleteToken(objectInput.getString("login"));

                user.setToken(null);
                return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body("{Successfully logout}");
            }

        }



        result.put("error", "Incorrect login or token");
        return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(result.toString());
    }


////////////////////////////////////////////////// END LOGOUT


    //////////////CHANGE PASSWORD


    /// create post method  url change password
    // input parameter login, old password , and new password  ,, token
//  validation old password and token

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

                resultJson.put("error", "Wrong password or token");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(resultJson.toString());

            } else {
                resultJson.put("error", "Wrong password or token");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(resultJson.toString());
            }

        } else {
            resultJson.put("error", "Wrong input");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(resultJson.toString());

        }

    }
///////////////////END


////////////////////// LOG history my login
    // input json login : peter and into the header put the token

    @RequestMapping(method = RequestMethod.POST, value = "/log")
    public ResponseEntity<String> log(@RequestBody String data, @RequestHeader(name = "Authorization") String token) throws JSONException {

        JSONObject obj = new JSONObject(data);
        JSONObject res = new JSONObject();

        if (obj.getString("login").isEmpty()) {
            res.put("error", "empty login ");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }

        User userObject = getUser(obj.getString("login"));


        if (userObject.getLogin() == null ) { // check we have user and check the token
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
                        res.put("message" , "empty database with your records ");
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
    public ResponseEntity<String> getLoggedUser(@RequestBody String data, @RequestHeader(name = "token") String token){
        org.json.JSONObject res = new org.json.JSONObject();
        org.json.JSONObject dat = new org.json.JSONObject(data);

        if (findToken(token)){
            Database db = new Database();
            res = db.getLoggedUser(dat.getString("login"));
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
        res.put("error", "token not defined");
        return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
    }

    private boolean findToken(String token) {
        Database db = new Database();
        return (db.findToken(token));
    }



    @RequestMapping("/users") //return all users
    public ResponseEntity<String> getUsers(@RequestHeader(name = "Authorization") String token, @RequestBody String data ) throws JSONException {


        JSONObject inputData = new JSONObject(data);
        String From = inputData.getString("login");

        System.out.println("This is From  " + From);

        if (inputData.getString("login") != null) {
            System.out.println("We have From " + From);
        }
        //JSONObject obj = new JSONObject(data);
        User userObject = getUser(From);


//
//        if (user == null || !database.existToken(token,user.getLogin())) { // check we have user and check the token
//            result.put("error", "Incorrect login or invalid TOKEN ");
//            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(result.toString());
//




        if (token == null) {
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"Bad request 123\"}");
        }
        Database database = new Database();
        if (existLogin(From) && database.existToken(token ,userObject.getLogin()) ) {

            List<String> users = database.getUsers();

            String jsonUsers = new Gson().toJson(users);

            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(jsonUsers);
        } else
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"Invalid token\"}");
    }

///////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////  POST  NEW MESSAGE
///

    /// from
    // to
    // messages
    @RequestMapping(method = RequestMethod.POST, value = "/message/new")
    public ResponseEntity<String> sendMessage(@RequestBody String data, @RequestHeader(name = "Authorization") String token) throws JSONException {


        JSONObject jsonObject = new JSONObject(data);
        JSONObject jsonResult = new JSONObject();

        User userObject = getUser(jsonObject.getString("from"));


        Database database = new Database();

        if (userObject.getLname() == null || !database.existToken(token,userObject.getLogin())) { // check we have user and check the token
            jsonResult.put("error", "Incorrect login or invalid TOKEN ");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(jsonResult.toString());
        }

        if (jsonObject.has("from") && jsonObject.has("message") && jsonObject.has("to")) {

            if (existLogin(jsonObject.getString("from")) && existLogin(jsonObject.getString("to"))) {
                // json add into array list

                String time = getTime();// actually time


                jsonResult.put("from", jsonObject.getString("from"));   // sender
                jsonResult.put("message", jsonObject.getString("message"));   // message
                jsonResult.put("to", jsonObject.getString("to"));   // acceptor
                jsonResult.put("datetime", time);   // acceptor


                System.out.println("message" + jsonResult.toString());
                JSONArray jsonArray = new JSONArray();
                jsonArray.put(jsonResult);

                System.out.println("jsonarray is  " + jsonArray);
                messages.add(jsonResult.toString());


                database.insertMessage(jsonResult);


                return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(jsonResult.toString());


            } else {
                jsonResult.put("error", "Acceptor or sender  dosnt exist in our database / list  ");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(jsonResult.toString());
            }
        } else {
            jsonResult.put("mistake", "empty message or acceptor or login sender ");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(jsonResult.toString());
        }


    }





/////////////////////////////////////// GET MESSAGES

    @RequestMapping(method = RequestMethod.GET, value = "/allmessages")
    public ResponseEntity<String> getMessages(@RequestBody String data, @RequestHeader(name = "Authorization") String token) throws JSONException {

        System.out.println("messages without fname ");

        JSONObject inputData = new JSONObject(data);
        JSONObject ResultJson = new JSONObject();

        User userObject = getUser(inputData.getString("login"));

        Database database = new Database();


        if (userObject.getLogin() == null || !database.existToken(token,userObject.getLogin())) { // check we have user and check the token

            ResultJson.put("error", "Incorrect login or invalid TOKEN ");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(ResultJson.toString());
        }

        if (inputData.has("login")) {

            if (existLogin(inputData.getString("login"))) {
                // json add into array list
                ResultJson.put("from", inputData.getString("login"));



                List<String> messagesList = database.getAllMessages();
                database.closeDatabase();



                return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(messagesList.toString());


            } else {
                ResultJson.put("error", "Acceptor or sender  dosnt exist in our database / list  ");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(ResultJson.toString());
            }
        } else {
            ResultJson.put("mistake", "empty LOGIN ");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(ResultJson.toString());
        }


    }


    //    @RequestMapping(method = RequestMethod.POST, value = "/playground")
    @RequestMapping( value = "/playground")

    public ResponseEntity<String> playground() throws JSONException {

        System.out.println("messages without fname ");

        // JSONObject inputData = new JSONObject(data);
        JSONObject ResultJson = new JSONObject();

        // User userObject = getUser(inputData.getString("login"));

        Database database = new Database();





        List<String> messagesList = database.getAllMessages();
        database.closeDatabase();

        //  put messages into json
                /*for (int i = 0; i < messages.size(); i++) {
                    ResultJson.put("message " + i, messages.get(i));
                    System.out.print(messages.get(i));
                }*/



        return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(messagesList.toString());



    }



    //////////////////////////////////////////////////////////////////
    // message only FROM ivana this message which send me sender
    @RequestMapping( method = RequestMethod.POST,value = "/messages")  //localhost:8080/messages?from=ivana
    public ResponseEntity<String> getMessage(@RequestBody String data , @RequestHeader(name = "Authorization") String token) throws JSONException {

        // System.out.println("message from  " + from + "  space  my login " + myLogin);
        // from
        // myLogin



        //  JSONObject obj = new JSONObject(data);
        JSONObject resultJson = new JSONObject();
        JSONObject inputData = new JSONObject(data);



        //User userObject = getUser(myLogin);
        User userObject = getUser(inputData.getString("myLogin"));


        Database database = new Database();

        // System.out.println("user object .geLogin  is "  + userObject.getLogin());

        //System.out.println(" vypis podmienky " + database.existToken(token, userObject.getLogin()));
        if (userObject == null || !database.existToken(token,userObject.getLogin())) {
            resultJson.put("error", "Incorrect login or invalid TOKEN ");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(resultJson.toString());
        }

        if (existLogin(inputData.getString("from"))) {

            // return messages only with from no all messages
            List<String> messagesList  = database.getMessagesFromUser(inputData.getString("from"), inputData.getString("myLogin"));

            database.closeDatabase();

            //System.out.println("your from is " + from);
            return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(messagesList.toString());


            //return null;
        } else {
            // todo change return empty json
            System.out.println("user dosnt exist ");
            resultJson.put("error", " input from do not exist in our database/ list ");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(resultJson.toString());

        }

    }


    /////////////////////// DELETE ACCOUNT   vytvorit DELETE request localhost:8080/delete/login
// todo potrebne dokncit
    @RequestMapping(method = RequestMethod.DELETE, value = "/delete/{login}")
    public ResponseEntity<String> deleteAccount(@PathVariable String login, @RequestHeader(name = "Authorization") String token) throws JSONException {
        // login your name
        // body data are empty
        //check the token


        User user = getUser(login);
        JSONObject result = new JSONObject();
        JSONObject jsonObject;
        //JSONObject list = new JSONObject(list);

        System.out.println("temp " + user);
        System.out.println("delete/login" + login);


        Database database = new Database();

        if (user.getLogin() == null || !database.existToken(token,user.getLogin())) { // check we have user and check the token
            result.put("error", "Incorrect login or invalid TOKEN ");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(result.toString());
        } else {

            // check the login exist
            if (existLogin(login)) {
                //delete from array list


                database.deleteUser(user.getLogin(),token);
                result.put("message", "Login delete successfully");

                return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(result.toString());


                /*for (int i = 0; i < list.size(); i++) {
                    jsonObject = new JSONObject(list.get(i));
                    if (jsonObject.getString("from").equals(login)) {
                        list.remove(list.get(i));
                    } else {
                    }
                }*/


            } else {
                result.put("error", "login do not exist in our database or list  ");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(result.toString());
            }
        }
        // return null;

    }





    /*vytvorit PATCH request localhost:8080/update/login
pricom login bude nase meno, header ma token.
v Body bude udaje co chceme zmenit, a to moze byt len fname alebo lname (prip obe)*/

    @RequestMapping(method = RequestMethod.PATCH, value = "/update") // here
    public ResponseEntity<String> updateLogin( @RequestBody String data, @RequestHeader(name = "Authorization") String token) throws JSONException {


        // System.out.println("temp " + user);

        //System.out.println("UPDATE/login " + login);


        JSONObject bodyData = new JSONObject(data);
        JSONObject result = new JSONObject();
        User user = getUser(bodyData.getString("login"));

        String login = bodyData.getString("login");
        // pricom login bude nase meno, header ma token.
        //v Body bude udaje co chceme zmenit, a to moze byt len fname alebo lname (prip obe)

        // I have fname

        Database database = new Database();


        if (user == null || !database.existToken(token,user.getLogin())) { // check we have user and check the token
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

                    database.updateuser( bodyData.getString("fname") ,  bodyData.getString("lname"),bodyData.getString("login"));
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


    public boolean checkPassword(String login, String password) throws JSONException {

        User user = getUser(login);
        if (user.getLogin() != null) {
            System.out.println("---                                             ----");
            System.out.println("password function check password is " + user.getPassword());
            System.out.println("---                                             ----");

            return BCrypt.checkpw(password, user.getPassword());
        }
        return false;
    }


    public static String generateNewToken() {
        Random rand = new Random();
        long longToken = Math.abs(rand.nextLong());
        String random = Long.toString(longToken, 16);

        return random;
    }


    private User getUser(String login) throws JSONException {
        Database database = new Database();

        JSONObject userJsonObject = database.getUser(login);

        if (userJsonObject != null) {


            System.out.println("userJsonobject " + userJsonObject);

            //create new user

            return new User(userJsonObject.getString("fname"),
                    userJsonObject.getString("lname"),
                    userJsonObject.getString("login"),
                    userJsonObject.getString("password"));
        } else {
            return null;
        }


    }


    private String getTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("ddMMyy HH:mm:ss");
        LocalDateTime localTime = LocalDateTime.now();
        String time;
        return time = dtf.format(localTime);
    }



}