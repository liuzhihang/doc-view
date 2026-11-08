package poc;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "fictional-inventory", url = "https://example.invalid")
@RequestMapping("/remote")
public interface FictionalFeignClient {
    @PostMapping("/items")
    String create(@RequestParam("dryRun") boolean dryRun);
}
