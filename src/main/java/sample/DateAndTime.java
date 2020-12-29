package sample;

import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@RestController
public class DateAndTime {
    List<User> list = new ArrayList<User>();
    List<String> log = new ArrayList<>();


    public DateAndTime() {
        list.add(new User("Roman", "sagan", "roman", "heslo"));
    }


    // todo create the hash bcrypt

    @RequestMapping("/time")
    public String getTime() {




        // ak niesom prihlaseny tak cas my nevypise neautorazovani pristup
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        System.out.println(sdf.format(cal.getTime()));


        return sdf.format(cal.getTime());
    }

    @RequestMapping("/primenumber/{number}")
    public String checkPrimeNumber(@PathVariable int number) {


        boolean flag = false;
        for (int i = 2; i <= number / 2; ++i) {
            // condition for nonprime number
            if (number % i == 0) {
                flag = true;
                break;
            }
        }

        if (!flag) {
            //   System.out.println(num + " is a prime number.");
            return "true"; // todo change return JSON
        } else {
            //System.out.println(num + " is not a prime number.");
            return "false";
        }

    }


    @RequestMapping("/time/hour")
    public String getHour() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH");
        System.out.println(sdf.format(cal.getTime()));
        return sdf.format(cal.getTime());
    }

    @RequestMapping("/hello")
    public String getHello() {
        return "Hello. How are you? ";
    }

    @RequestMapping("/hello/{name}")
    public String getHelloWithName(@PathVariable String name) {
        return "Hello " + name + ". How are you? ";
    }

    @RequestMapping("/hi") // GET method default
    public String getHi(@RequestParam(value = "fname") String fname, @RequestParam(value = "age") String age) {
        return "Hello. How are you? Your name is " + fname + " and you are " + age;

    }
    // todo and also Generate token
    // todo compare the password if is ok return object json with all data fName lastn name and so on
    // when you logout  delete token   need token and login name






    // todo every user can see all other user with account without password, only user with account can see
    // user?token and login  return all users

    // users/login novak  return only information about novak j  ,, about one user
}