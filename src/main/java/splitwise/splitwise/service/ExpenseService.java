package splitwise.splitwise.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import splitwise.splitwise.dto.AddExpenseRequest;
import splitwise.splitwise.dto.ExpenseSplitResponse;
import splitwise.splitwise.exception.ExpenseNotFound;
import splitwise.splitwise.exception.GroupNotFound;
import splitwise.splitwise.exception.UserNotFound;
import splitwise.splitwise.model.*;
import splitwise.splitwise.repository.*;
import splitwise.splitwise.strategy.SplitStrategy;
import splitwise.splitwise.strategy.SplitStrategyFactory;

import java.util.*;
import java.util.stream.Collectors;


public interface ExpenseService {

    public Expense addExpense(Long groupid, AddExpenseRequest request);

    public List<Expense> getExpensesByGroup(Long groupid);

    public List<Expense> getExpensesByGroupAndUser(Long groupid, Long userid);

    public List<String> getBalances(Long groupid);

    public List<String> getUserBalance(Long groupid, Long userid);

    public double getBalanceBetweenUsers(Long groupId , Long user1Id , Long user2Id);

    public void deleteExpense(Long groupid,Long expenseid);

    public Expense updateExpense(Long expenseid, AddExpenseRequest request);

    public Expense getExpenseByGroupAndId(Long groupid, Long expenseid);

    public List<ExpenseSplitResponse> getExpenseSplitsByGroupAndExpenseId(Long groupid, Long expenseid);
}
