package splitwise.splitwise.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import splitwise.splitwise.dto.UpdateSettlementRequest;
import splitwise.splitwise.exception.*;
import splitwise.splitwise.model.ExpenseGroup;
import splitwise.splitwise.model.Settlement;
import splitwise.splitwise.model.User;
import splitwise.splitwise.repository.ExpenseGroupRepository;
import splitwise.splitwise.repository.SettlementRepository;
import splitwise.splitwise.repository.UserRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;


public interface SettlementService {


    // create settlement
    public Settlement createSettlement(Long groupid, Long paidbyId, Long paidtoId, Double amount);

    //get settlement in a group by groupid
    public List<Settlement> getSettlementsByGroup(Long groupid);

    // get settelements made by a user
    public List<Settlement> getSettlementsPaidByUser(Long groupid, Long userid);

    // get settlement made to a user
    public List<Settlement> getSettlementsPaidToUser(Long groupid, Long userid);

    public void deleteSettlement(Long settlementid);

    public Settlement updateSettlement(Long settlementid, UpdateSettlementRequest request);
}
