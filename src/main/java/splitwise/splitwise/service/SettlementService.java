package splitwise.splitwise.service;

import java.util.List;

import splitwise.splitwise.dto.UpdateSettlementRequest;
import splitwise.splitwise.model.Settlement;


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
