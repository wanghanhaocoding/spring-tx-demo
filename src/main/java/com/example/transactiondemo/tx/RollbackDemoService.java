package com.example.transactiondemo.tx;

import com.example.transactiondemo.account.Account;
import com.example.transactiondemo.account.AccountMapper;
import com.example.transactiondemo.tx.exception.BusinessFailureException;
import com.example.transactiondemo.tx.exception.CheckedTransferException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class RollbackDemoService {

    private final AccountMapper accountMapper;

    public RollbackDemoService(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    @Transactional
    public void runtimeExceptionRollback(Long fromId, Long toId, BigDecimal amount) {
        move(fromId, toId, amount);
        throw new BusinessFailureException("RuntimeException 默认回滚");
    }

    @Transactional
    public void checkedExceptionNoRollback(Long fromId, Long toId, BigDecimal amount) throws CheckedTransferException {
        move(fromId, toId, amount);
        throw new CheckedTransferException("Checked Exception 默认不回滚");
    }

    @Transactional(rollbackFor = CheckedTransferException.class)
    public void checkedExceptionWithRollback(Long fromId, Long toId, BigDecimal amount) throws CheckedTransferException {
        move(fromId, toId, amount);
        throw new CheckedTransferException("声明 rollbackFor 后回滚");
    }

    private void move(Long fromId, Long toId, BigDecimal amount) {
        Account from = getAccount(fromId);
        Account to = getAccount(toId);
        from.withdraw(amount);
        to.deposit(amount);
        accountMapper.updateBalance(from.getId(), from.getBalance());
        accountMapper.updateBalance(to.getId(), to.getBalance());
    }

    private Account getAccount(Long id) {
        Account account = accountMapper.findById(id);
        if (account == null) {
            throw new IllegalArgumentException("Account not found: " + id);
        }
        return account;
    }
}
