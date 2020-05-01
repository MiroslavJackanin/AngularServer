package sample;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MathController {

    @RequestMapping("/math")
    public  String getString(){
        return "Hello, I am Math.";
    }

    @RequestMapping("/math/{value}")
    public String getString2(@PathVariable int id){
        return "Hello Math " + id;
    }
}
