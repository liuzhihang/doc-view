package poc;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SpringController {
    @GetMapping("/users/{id}")
    public String findUser(
            @PathVariable("id") String id,
            @RequestParam(name = "q") String query,
            @RequestHeader("X-Trace") String trace) {
        return "fictional";
    }
}
