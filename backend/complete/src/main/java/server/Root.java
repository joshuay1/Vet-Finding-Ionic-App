package server;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
public class Root {
    
    @RequestMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }

    
}
