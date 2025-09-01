package splitwise.splitwise.strategy.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import splitwise.splitwise.model.Expense;
import splitwise.splitwise.model.ExpenseSplit;
import splitwise.splitwise.strategy.SplitStrategy;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class EqualSplitStrategy implements SplitStrategy {

    @Override
    public List<ExpenseSplit> calculateSplits(Expense expense, Long paidById , List<Long> participantsIds, List<Double> values){

        log.info("Calculating Equal splits for Expense ID: {}, Paid by User ID: {}",expense.getExpenseid(),paidById);
        List<ExpenseSplit> splits = new ArrayList<>();
        double share = expense.getAmount()/participantsIds.size();

        for (Long userId : participantsIds) {
            if (!userId.equals(paidById)){
                splits.add(new ExpenseSplit(expense.getExpenseid(),userId,share));
                log.info("Assigned share {} to User ID: {} for Expense ID: {}",share,userId,expense.getExpenseid());
            }
        }
        log.info("Split calculation completed for Expense ID: {}, Total splits created: {}",
                expense.getExpenseid(),splits.size());
        return splits;
    }
}


