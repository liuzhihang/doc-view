package contract.spring;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口
 */
@RestController
@RequestMapping("/api")
public class BasicController {

    /**
     * 按编号查询用户。
     *
     * @param id 用户编号
     * @DocView.Name 查询用户
     */
    @GetMapping("/users")
    public UserResponse getUser(@RequestParam String id) {
        return null;
    }
}

class UserResponse {

    /**
     * 用户名称
     */
    private String name;
}
