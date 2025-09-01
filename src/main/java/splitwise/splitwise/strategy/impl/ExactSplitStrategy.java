package splitwise.splitwise.strategy.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import splitwise.splitwise.exception.ExactAmountSum;
import splitwise.splitwise.exception.ParticipantCountMismatch;
import splitwise.splitwise.model.Expense;
import splitwise.splitwise.model.ExpenseSplit;
import splitwise.splitwise.strategy.SplitStrategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ExactSplitStrategy implements SplitStrategy {

    @Override
    public List<ExpenseSplit> calculateSplits(Expense expense, Long paidById , List<Long> participantsIds, List<Double> exactAmounts) {

        log.info("Calculating Exact splits for Expense ID: {}, Paid by User ID: {}",expense.getExpenseid(),paidById);
        if (participantsIds.size() != exactAmounts.size()){
            throw new ParticipantCountMismatch("Participants and exact amount count mismatch.");
        }

        double total = exactAmounts.stream().mapToDouble(Double::doubleValue).sum();
        if (Double.compare(total, expense.getAmount()) != 0) {
            throw new ExactAmountSum("Exact amounts do not sum to total expense.");
        }

        List<ExpenseSplit> splits = new ArrayList<>();
        for (int i = 0; i< participantsIds.size();i++){
            if (!participantsIds.get(i).equals(expense.getUserid().getUserid())){
                double roundedAmount = roundToOneDecimal(exactAmounts.get(i));
                splits.add(new ExpenseSplit(expense.getExpenseid(), participantsIds.get(i),roundedAmount));
                log.info("Assigned amount {} to User ID: {} for Expense ID: {}",roundedAmount,participantsIds.get(i),expense.getExpenseid());
            }
        }
        log.info("Split calculation completed for Expense ID: {}, Total splits created: {}",
                expense.getExpenseid(),splits.size());
        return splits;
    }

    private double roundToOneDecimal(double value){
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
