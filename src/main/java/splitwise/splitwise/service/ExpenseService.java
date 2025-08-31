package splitwise.splitwise.service;


import java.util.List;

import splitwise.splitwise.dto.AddExpenseRequest;
import splitwise.splitwise.dto.ExpenseSplitResponse;
import splitwise.splitwise.model.Expense;


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
