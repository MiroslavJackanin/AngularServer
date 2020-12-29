/*package sample;

import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExampleController {

    @RequestMapping("/hello")
    public String getHello(){
        return "Hello. How are you? ";
    }

    @RequestMapping("/hello/{name}")
    public String getHelloWithName(@PathVariable String name){
        return "Hello "+name+". How are you? ";
    }

    @RequestMapping("/hi")
    public String getHi(@RequestParam(value="fname") String fname, @RequestParam(value="age") String age){
        return "Hello. How are you? Your name is "+fname+" and you are "+age;
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
            } else
                isPrimeNumber=false;

            JSONObject res = new JSONObject();
            res.put("number",value);
            res.put("primenumber",isPrimeNumber);

            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }catch(NumberFormatException e) {
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"Param must be integer\"}");
        }
    }
}*/
