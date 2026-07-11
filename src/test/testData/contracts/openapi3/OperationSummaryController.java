package contract.openapi3;

import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class OperationSummaryController {

    @Operation(summary = "查询用户", description = "按 ID 查询用户")
    @GetMapping("/summary")
    public String summaryOnly() {
        return "";
    }

    @Operation(summary = "OpenAPI 3 名称")
    @ApiOperation(value = "Swagger 2 名称")
    @GetMapping("/priority")
    public String summaryPriority() {
        return "";
    }

    @Operation(summary = "   ")
    @ApiOperation(value = "空白 summary fallback")
    @GetMapping("/blank")
    public String blankSummary() {
        return "";
    }

    @Operation(summary = "")
    @ApiOperation(value = "空 summary fallback")
    @GetMapping("/empty")
    public String emptySummary() {
        return "";
    }

    @Operation
    @ApiOperation(value = "默认 summary fallback")
    @GetMapping("/default")
    public String defaultSummary() {
        return "";
    }

    @Operation(summary = "应被忽略")
    @ApiOperation(value = "Swagger 3 已关闭")
    @GetMapping("/disabled")
    public String swagger3Disabled() {
        return "";
    }
}
