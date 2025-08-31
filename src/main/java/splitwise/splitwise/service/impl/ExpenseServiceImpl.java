package splitwise.splitwise.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import splitwise.splitwise.dto.AddExpenseRequest;
import splitwise.splitwise.dto.ExpenseSplitResponse;
import splitwise.splitwise.exception.ExpenseNotFound;
import splitwise.splitwise.exception.GroupNotFound;
import splitwise.splitwise.exception.UserNotFound;
import splitwise.splitwise.model.Expense;
import splitwise.splitwise.model.ExpenseGroup;
import splitwise.splitwise.model.ExpenseSplit;
import splitwise.splitwise.model.Settlement;
import splitwise.splitwise.model.User;
import splitwise.splitwise.repository.ExpenseGroupRepository;
import splitwise.splitwise.repository.ExpenseRepository;
import splitwise.splitwise.repository.ExpenseSplitRepository;
import splitwise.splitwise.repository.GroupMemberRepository;
import splitwise.splitwise.repository.SettlementRepository;
import splitwise.splitwise.repository.UserRepository;
import splitwise.splitwise.service.ExpenseService;
import splitwise.splitwise.strategy.SplitStrategy;
import splitwise.splitwise.strategy.SplitStrategyFactory;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final ExpenseGroupRepository groupRepository;
    private final ExpenseSplitRepository splitRepository;
    private final SettlementRepository settlementRepository;
    private final SplitStrategyFactory splitStrategyFactory;
    private final GroupMemberRepository groupMemberRepository;



    // Add and split expense based on the strategy among group members

    @Override
    @Transactional
    public Expense addExpense(Long groupid, AddExpenseRequest request) {
        log.info("Adding expense: groupId={}, amount={}, paidBy={}, participants={}", groupid, request.getAmount(), request.getPaidBy(), request.getParticipants().size());
        ExpenseGroup group = groupRepository.findById(groupid)
                .orElseThrow(() -> new GroupNotFound("Group not found"));
        User payer = userRepository.findById(request.getPaidBy())
                .orElseThrow(() -> new UserNotFound("User not found"));

        List<User> participants = request.getParticipants().stream()
                .map(userId -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new UserNotFound("Participant with ID " + userId + " not found"));

                    if (!groupMemberRepository.existsById_GroupidAndId_Userid(groupid, userId)) {
                        throw new UserNotFound("User ID " + userId + " is not a member of the group");
                    }

                    return user;
                })
                .toList();

        // Save expense
        Expense expense = new Expense();
        expense.setAmount(request.getAmount());
        expense.setDescription(request.getDescription());
        expense.setGroupid(group);
        expense.setUserid(payer);
        expense.setExpensedate(new java.sql.Timestamp(System.currentTimeMillis()));

        Expense savedExpense = expenseRepository.save(expense);
        log.debug("Saved expense with id={}", savedExpense.getExpenseid());

        // strategy call
        SplitStrategy strategy = splitStrategyFactory.getStrategy(request.getSplitType());
        List<ExpenseSplit> splits = strategy.calculateSplits(
                savedExpense,
                request.getPaidBy(),
                request.getParticipants(),
                request.getValues()
        );

        splitRepository.saveAll(splits);
        log.info("Expense added and splits saved: expenseId={}, splitsCount={}", savedExpense.getExpenseid(), splits.size());

        return savedExpense;
    }

    //Get all expenses in a group
    @Override
    public List<Expense> getExpensesByGroup(Long groupid) {
        log.debug("Fetching expenses for groupId={}", groupid);
        ExpenseGroup group = groupRepository.findById(groupid)
                .orElseThrow(() -> new GroupNotFound("Group not found"));
        return expenseRepository.findByGroupid_Groupid(groupid);
    }

    // Get all expenses added by a user in a group

    @Override
    public List<Expense> getExpensesByGroupAndUser(Long groupid, Long userid) {
        log.debug("Fetching expenses for groupId={} by userId={}", groupid, userid);
        ExpenseGroup group = groupRepository.findById(groupid)
                .orElseThrow(() -> new GroupNotFound("Group not found"));
        User payer = userRepository.findById(userid)
                .orElseThrow(() -> new UserNotFound("User not found"));
        return expenseRepository.findByGroupid_GroupidAndUserid_Userid(groupid, userid);
    }


    //Return balances in the format: Rahul owes Parth ₹1000

    @Override
    public List<String> getBalances(Long groupid) {
        log.debug("Computing balances for groupId={}", groupid);
        List<Expense> expenses = expenseRepository.findByGroupid_Groupid(groupid);
        List<Settlement> settlements = settlementRepository.findByGroupid_Groupid(groupid);

        Map<Long, Map<Long, Double>> balanceMap = buildBalanceMap(expenses);
        applySettlements(balanceMap, settlements);
        Map<String, Double> finalBalances = computeNetBalances(balanceMap);

        List<String> result = balancesFormat(finalBalances);
        log.info("Computed balances for groupId={} entries={}", groupid, result.size());
        return result;

    }
    // Step 1: Add dues from splits
    private Map<Long , Map<Long , Double>> buildBalanceMap(List<Expense> expenses) {
        Map<Long,Map<Long ,Double>> balanceMap = new HashMap<>();
        for (Expense expense : expenses) {
            Long paidBy = expense.getUserid().getUserid();
            List<ExpenseSplit> splits = splitRepository.findByExpenseid(expense.getExpenseid());

            for (ExpenseSplit split : splits) {
                Long owedBy = split.getUserid();
                double amount = split.getAmount();

                balanceMap
                        .computeIfAbsent(owedBy, k -> new HashMap<>())
                        .merge(paidBy, amount, Double::sum);
            }
        }
        return balanceMap;
    }

    // Step 2: Subtract settlements
    public void applySettlements(Map<Long,Map<Long,Double>> balanceMap,List<Settlement> settlements) {
        log.debug("Applying {} settlements", settlements.size());
        for (Settlement settlement : settlements) {
            Long from = settlement.getPaidby().getUserid();
            Long to = settlement.getPaidto().getUserid();
            double amount = settlement.getAmount();


            boolean vaildSettlement = balanceMap.containsKey(from) && balanceMap.get(from).containsKey(to);

            if(vaildSettlement){
                balanceMap
                        .computeIfAbsent(from, k -> new HashMap<>())
                        .merge(to, -amount, Double::sum);

            }else {
                balanceMap
                        .computeIfAbsent(to, k -> new HashMap<>())
                        .merge(from, amount, Double::sum);
            }

        }
    }

    // Step 3: net calculation
    private Map<String, Double> computeNetBalances(Map<Long,Map<Long,Double>> balanceMap) {
        Map<String, Double> finalBalances = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Double>> debtorEntry : balanceMap.entrySet()) {
            Long debtor = debtorEntry.getKey();

            for (Map.Entry<Long, Double> creditorEntry : debtorEntry.getValue().entrySet()) {
                Long creditor = creditorEntry.getKey();
                double amount = creditorEntry.getValue();

                if (amount == 0) continue;

                String key,reversekey;
                double finalAmount;

                if (amount > 0) {
                    key = debtor + " -> " + creditor;
                    reversekey = creditor + " -> " + debtor;
                    finalAmount = amount;
                } else {
                    // Reverse the direction
                    key = creditor + " -> " + debtor;
                    reversekey = debtor + " -> " + creditor;
                    finalAmount = -amount;
                }

                if (finalBalances.containsKey(reversekey)) {
                    double reverseAmount = finalBalances.get(reversekey);
                    if (reverseAmount > finalAmount) {
                        finalBalances.put(reversekey, reverseAmount - finalAmount);
                    } else if (reverseAmount < finalAmount) {
                        finalBalances.remove(reversekey);
                        finalBalances.put(key, finalAmount - reverseAmount);
                    } else {
                        finalBalances.remove(reversekey);
                    }
                } else {
                    finalBalances.put(key, finalAmount);
                }

            }
        }
        return finalBalances;
    }

    // Step 4: Convert to readable output
    private List<String> balancesFormat(Map<String ,Double> finalBalances){
        List<String> balances = new ArrayList<>();

        for (Map.Entry<String, Double> entry : finalBalances.entrySet()) {
            String[] ids = entry.getKey().split(" -> ");
            Long debtorId = Long.parseLong(ids[0]);
            Long creditorId = Long.parseLong(ids[1]);
            double amount = entry.getValue();

            String debtorName = userRepository.findById(debtorId).orElseThrow().getUsername();
            String creditorName = userRepository.findById(creditorId).orElseThrow().getUsername();
            balances.add(debtorName + " owes " + creditorName + " ₹" + String.format("%.2f", amount));
        }

        return balances;
    }


    // get balance for a specific user in a group (balance controller)
    @Override
    public List<String> getUserBalance(Long groupid, Long userid){
        log.debug("Computing user balance: groupId={}, userId={}", groupid, userid);
        List<String> allBalances = getBalances(groupid);
        String username = userRepository.findById(userid)
                .orElseThrow(() -> new UserNotFound("User not found")).getUsername();

        List<String> userBalances = new ArrayList<>();
        for (String balance : allBalances){
            if (balance.contains(username)){
                userBalances.add(balance);
            }
        }
        return userBalances;
    }

    //balance between 2 users
    @Override
    public double getBalanceBetweenUsers(Long groupId , Long user1Id , Long user2Id){
        log.debug("Computing balance between users: groupId={}, user1Id={}, user2Id={}", groupId, user1Id, user2Id);
        List<String> balances = getBalances(groupId);

        String user1Touser2Prefix = getUsername(user1Id) + " owes " + getUsername(user2Id) + " ₹";
        String user2Touser1Prefix = getUsername(user2Id) + " owes " + getUsername(user1Id) + " ₹";

        for (String balanceLine : balances){
            if (balanceLine.startsWith(user1Touser2Prefix)){
                String amountstr = balanceLine.substring(user1Touser2Prefix.length());
                return Double.parseDouble(amountstr);
            }else if (balanceLine.startsWith(user2Touser1Prefix)){
                String amountstr = balanceLine.substring(user2Touser1Prefix.length());
                return -Double.parseDouble(amountstr);
            }
        }
        return 0.0;

    }

    // username from userid
    private String getUsername(Long userId){
        return userRepository.findById(userId)
                .orElseThrow(()-> new UserNotFound("User not found"))
                .getUsername();
    }

    // delete expense
    @Override
    @Transactional
    public void deleteExpense(Long groupid,Long expenseid) {
        log.info("Deleting expense: groupId={}, expenseId={}", groupid, expenseid);
        Expense expense = expenseRepository.findById(expenseid)
                .orElseThrow(() -> new ExpenseNotFound("Expense not found with ID: "));

        // Delete splits associated with the expense
        splitRepository.deleteByExpenseid(expense.getExpenseid());

        // Delete the expense
        expenseRepository.delete(expense);
        log.info("Deleted expense: expenseId={}", expenseid);

    }

    //update expense
    @Override
    @Transactional
    public Expense updateExpense(Long expenseid, AddExpenseRequest request) {
        log.info("Updating expense: expenseId={}, amount={}, paidBy={}", expenseid, request.getAmount(), request.getPaidBy());
        Expense existingExpense = expenseRepository.findById(expenseid)
                .orElseThrow(() -> new ExpenseNotFound("Expense not found"));

        ExpenseGroup group = existingExpense.getGroupid();

        User payer = userRepository.findById(request.getPaidBy())
                .orElseThrow(() -> new UserNotFound("User not found"));

        List<User> participants = request.getParticipants().stream()
                .map(userId -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new UserNotFound("Participant with ID " + userId + " not found"));

                    if (!groupMemberRepository.existsById_GroupidAndId_Userid(group.getGroupid(), userId)) {
                        throw new UserNotFound("User ID " + userId + " is not a member of the group");
                    }

                    return user;
                })
                .toList();

        // Update fields
        existingExpense.setAmount(request.getAmount());
        existingExpense.setDescription(request.getDescription());
        existingExpense.setUserid(payer);
        existingExpense.setExpensedate(new java.sql.Timestamp(System.currentTimeMillis()));

        // Delete old splits
        splitRepository.deleteByExpenseid(existingExpense.getExpenseid());

        // Save updated expense
        Expense updatedExpense = expenseRepository.save(existingExpense);
        log.debug("Updated expense saved: expenseId={}", updatedExpense.getExpenseid());

        // Recalculate and save new splits
        SplitStrategy strategy = splitStrategyFactory.getStrategy(request.getSplitType());
        List<ExpenseSplit> newSplits = strategy.calculateSplits(
                updatedExpense,
                request.getPaidBy(),
                request.getParticipants(),
                request.getValues()
        );

        splitRepository.saveAll(newSplits);
        log.info("Updated splits saved: expenseId={}, splitsCount={}", updatedExpense.getExpenseid(), newSplits.size());

        return updatedExpense;
    }

    @Override
    public Expense getExpenseByGroupAndId(Long groupid, Long expenseid) {
        log.debug("Fetching expense by id: groupId={}, expenseId={}", groupid, expenseid);
        Expense expense = expenseRepository.findById(expenseid)
                .orElseThrow(() -> new ExpenseNotFound("Expense not found"));

        if (!expense.getGroupid().getGroupid().equals(groupid)) {
            throw new GroupNotFound("Expense does not belong to the specified group");
        }

        return expense;
    }

    @Override
    public List<ExpenseSplitResponse> getExpenseSplitsByGroupAndExpenseId(Long groupid, Long expenseid) {
        log.debug("Fetching expense splits: groupId={}, expenseId={}", groupid, expenseid);
        Expense expense = expenseRepository.findById(expenseid)
                .orElseThrow(() -> new ExpenseNotFound("Expense not found"));

        if (!expense.getGroupid().getGroupid().equals(groupid)) {
            throw new GroupNotFound("Expense does not belong to the specified group");
        }

        List<ExpenseSplit> splits = splitRepository.findByExpenseid(expenseid);

        return splits.stream()
                .map(split -> {
                    User user = userRepository.findById(split.getUserid())
                            .orElseThrow(() -> new UserNotFound("User with ID " + split.getUserid() + " not found"));
                    return new ExpenseSplitResponse(user.getUsername(), split.getAmount());
                })
                .collect(Collectors.toList());
    }

}
