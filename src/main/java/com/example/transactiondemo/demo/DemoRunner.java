package com.example.transactiondemo.demo;

import com.example.transactiondemo.account.AccountService;
import com.example.transactiondemo.audit.AuditService;
import com.example.transactiondemo.tx.PropagationDemoService;
import com.example.transactiondemo.tx.ProxyBoundaryDemoService;
import com.example.transactiondemo.tx.RollbackDemoService;
import com.example.transactiondemo.tx.exception.CheckedTransferException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConditionalOnProperty(name = "demo.runner.enabled", havingValue = "true", matchIfMissing = true)
// 启动时顺序打印所有事务示例，方便直接观察结果。
public class DemoRunner implements CommandLineRunner {

    private final AccountService accountService;
    private final RollbackDemoService rollbackDemoService;
    private final PropagationDemoService propagationDemoService;
    private final ProxyBoundaryDemoService proxyBoundaryDemoService;
    private final AuditService auditService;

    public DemoRunner(AccountService accountService,
                      RollbackDemoService rollbackDemoService,
                      PropagationDemoService propagationDemoService,
                      ProxyBoundaryDemoService proxyBoundaryDemoService,
                      AuditService auditService) {
        this.accountService = accountService;
        this.rollbackDemoService = rollbackDemoService;
        this.propagationDemoService = propagationDemoService;
        this.proxyBoundaryDemoService = proxyBoundaryDemoService;
        this.auditService = auditService;
    }

    @Override
    public void run(String... args) {
        printTitle("1. REQUIRED：默认传播行为，成功转账会提交");
        accountService.transfer(1L, 2L, new BigDecimal("100.00"));
        printBalances();

        printTitle("2. RuntimeException：默认回滚");
        try {
            rollbackDemoService.runtimeExceptionRollback(1L, 2L, new BigDecimal("50.00"));
        } catch (Exception ex) {
            System.out.println("捕获异常: " + ex.getMessage());
        }
        printBalances();

        printTitle("3. Checked Exception：默认不回滚");
        try {
            rollbackDemoService.checkedExceptionNoRollback(1L, 2L, new BigDecimal("30.00"));
        } catch (CheckedTransferException ex) {
            System.out.println("捕获异常: " + ex.getMessage());
        }
        printBalances();

        printTitle("4. rollbackFor：让 checked exception 也回滚");
        try {
            rollbackDemoService.checkedExceptionWithRollback(1L, 2L, new BigDecimal("20.00"));
        } catch (CheckedTransferException ex) {
            System.out.println("捕获异常: " + ex.getMessage());
        }
        printBalances();

        printTitle("5. REQUIRES_NEW：外层失败，内层新事务仍可提交");
        try {
            propagationDemoService.outerRequiredThenRequiresNewAndFail(1L, 2L, new BigDecimal("10.00"));
        } catch (Exception ex) {
            System.out.println("捕获异常: " + ex.getMessage());
        }
        printBalances();
        System.out.println("审计日志条数: " + auditService.count());

        printTitle("6. MANDATORY：没有外层事务会失败");
        try {
            propagationDemoService.callMandatoryWithoutTransaction();
        } catch (Exception ex) {
            System.out.println("捕获异常: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
        }

        printTitle("7. MANDATORY：有外层事务时正常执行");
        propagationDemoService.callMandatoryWithinTransaction();
        System.out.println("审计日志条数: " + auditService.count());

        printTitle("8. SUPPORTS：没有事务也能执行");
        propagationDemoService.callSupportsWithoutTransaction();
        System.out.println("审计日志条数: " + auditService.count());

        printTitle("9. SUPPORTS：有事务时加入外层，外层失败则一起回滚");
        try {
            propagationDemoService.outerRequiredThenSupportsAndFail(1L, 2L, new BigDecimal("10.00"));
        } catch (Exception ex) {
            System.out.println("捕获异常: " + ex.getMessage());
        }
        printBalances();
        System.out.println("审计日志条数: " + auditService.count());

        printTitle("10. NOT_SUPPORTED：挂起外层事务，外层失败也能保留结果");
        try {
            propagationDemoService.outerRequiredThenNotSupportedAndFail(1L, 2L, new BigDecimal("10.00"));
        } catch (Exception ex) {
            System.out.println("捕获异常: " + ex.getMessage());
        }
        printBalances();
        System.out.println("审计日志条数: " + auditService.count());

        printTitle("11. NEVER：无事务时可以执行");
        propagationDemoService.callNeverWithoutTransaction();
        System.out.println("审计日志条数: " + auditService.count());

        printTitle("12. NEVER：事务中调用会失败");
        try {
            propagationDemoService.callNeverWithinTransaction();
        } catch (Exception ex) {
            System.out.println("捕获异常: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
        }

        printTitle("13. NESTED：内层回滚到 savepoint，外层仍可提交");
        propagationDemoService.outerRequiredThenNestedFailButOuterCommit(1L, 2L, new BigDecimal("10.00"));
        printBalances();
        System.out.println("审计日志条数: " + auditService.count());

        printTitle("14. 代理边界：通过代理调用时事务生效");
        System.out.println("invokeThroughProxy() = " + proxyBoundaryDemoService.invokeThroughProxy());

        printTitle("15. self-invocation：同类内部 this 调用绕过代理，事务失效");
        System.out.println("invokeInsideSameBean() = " + proxyBoundaryDemoService.invokeInsideSameBean());

        printTitle("公司里最常用的传播机制");
        System.out.println("1) REQUIRED：最常见，绝大多数业务 Service 默认就是它。\n"
                + "2) REQUIRES_NEW：常用于审计日志、失败记录、补偿记录。\n"
                + "3) SUPPORTS：偶尔用于查询/辅助逻辑。\n"
                + "4) MANDATORY / NOT_SUPPORTED / NEVER：更多是约束调用契约。\n"
                + "5) NESTED：最少见，但最适合理解 savepoint。\n\n"
                + "关键区别：\n"
                + "- REQUIRES_NEW = 新开独立事务。\n"
                + "- NOT_SUPPORTED = 不在事务里运行。\n"
                + "- NESTED = 仍在同一个大事务里，但可以回滚到 savepoint。\n");
    }

    private void printBalances() {
        System.out.println("Alice balance = " + accountService.balanceOf(1L));
        System.out.println("Bob balance   = " + accountService.balanceOf(2L));
    }

    private void printTitle(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
