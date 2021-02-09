package sample;

import org.json.JSONException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MainController {
    public static void main(String[] args) throws JSONException {
        SpringApplication.run(MainController.class,args);
    }
}