package splitwise.splitwise.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;


import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import splitwise.splitwise.dto.UpdateSettlementRequest;
import splitwise.splitwise.exception.GroupNotFound;
import splitwise.splitwise.exception.NoDuesExist;
import splitwise.splitwise.exception.PayerAndPayeeSame;
import splitwise.splitwise.exception.SettleAmountMoreThanDue;
import splitwise.splitwise.exception.SettlementNotFound;
import splitwise.splitwise.exception.UserNotFound;
import splitwise.splitwise.model.ExpenseGroup;
import splitwise.splitwise.model.Settlement;
import splitwise.splitwise.model.User;
import splitwise.splitwise.repository.ExpenseGroupRepository;
import splitwise.splitwise.repository.SettlementRepository;
import splitwise.splitwise.repository.UserRepository;
import splitwise.splitwise.service.ExpenseService;
import splitwise.splitwise.service.SettlementService;

@Service
@Slf4j
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {
    private final SettlementRepository settlementRepository;
    private final UserRepository userRepository;
    private final ExpenseGroupRepository groupRepository;
    private final ExpenseService expenseService;

    // create settlement
    @Override
    public Settlement createSettlement(Long groupid, Long paidbyId, Long paidtoId, Double amount) {
        log.info("Creating settlement: groupId={}, paidBy={}, paidTo={}, amount={}", groupid, paidbyId, paidtoId, amount);
        if (paidbyId.equals(paidtoId)){
            throw new PayerAndPayeeSame("Payer and Payee cannot be the same user");
        }

        User paidby = userRepository.findById(paidbyId)
                .orElseThrow(() -> new UserNotFound("Payer not found"));

        User paidto = userRepository.findById(paidtoId)
                .orElseThrow(() -> new UserNotFound("Payee not found"));

        ExpenseGroup group = groupRepository.findById(groupid)
                .orElseThrow(() -> new GroupNotFound("Group not found"));

        double balance = expenseService.getBalanceBetweenUsers(groupid,paidbyId,paidtoId);
        if (balance <= 0.0){
            throw new NoDuesExist("No dues exist between these users");
        }

        if (amount > balance) {
            throw new SettleAmountMoreThanDue("Settlement amount exceeds dues. Due amount: " + balance);
        }

        Settlement settlement = new Settlement();
        settlement.setGroupid(group);
        settlement.setPaidby(paidby);
        settlement.setPaidto(paidto);
        settlement.setAmount(amount);
        settlement.setDate(Timestamp.valueOf(LocalDateTime.now()));

        Settlement saved = settlementRepository.save(settlement);
        log.info("Settlement created: settlementId={}, groupId={}", saved.getId(), groupid);
        return saved;
    }

    //get settlement in a group by groupid
    @Override
    public List<Settlement> getSettlementsByGroup(Long groupid) {
        log.debug("Fetching settlements for groupId={}", groupid);
        ExpenseGroup group = groupRepository.findById(groupid)
                .orElseThrow(() -> new GroupNotFound("Group not found"));
        return settlementRepository.findByGroupid_Groupid(groupid);
    }

    // get settelements made by a user
    @Override
    public List<Settlement> getSettlementsPaidByUser(Long groupid, Long userid){
        log.debug("Fetching settlements paid by user: groupId={}, userId={}", groupid, userid);
        ExpenseGroup group = groupRepository.findById(groupid)
                .orElseThrow(() -> new GroupNotFound("Group not found"));
        User paidby = userRepository.findById(userid)
                .orElseThrow(() -> new UserNotFound("Payer not found"));

        return settlementRepository.findByGroupid_GroupidAndPaidby_Userid(groupid,userid);
    }

    // get settlement made to a user
    @Override
    public List<Settlement> getSettlementsPaidToUser(Long groupid, Long userid){
        log.debug("Fetching settlements paid to user: groupId={}, userId={}", groupid, userid);
        ExpenseGroup group = groupRepository.findById(groupid)
                .orElseThrow(() -> new GroupNotFound("Group not found"));
        User paidto  = userRepository.findById(userid)
                .orElseThrow(() -> new UserNotFound("Payee not found"));

        return settlementRepository.findByGroupid_GroupidAndPaidto_Userid(groupid,userid);
    }

    @Override
    public void deleteSettlement(Long settlementid) {
        log.info("Deleting settlement: settlementId={}", settlementid);
        Settlement settlement = settlementRepository.findById(settlementid)
                .orElseThrow(() -> new SettlementNotFound("Settlement not found"));
        settlementRepository.delete(settlement);
        log.info("Deleted settlement: settlementId={}", settlementid);
    }

    @Override
    public Settlement updateSettlement(Long settlementid, UpdateSettlementRequest request) {
        log.info("Updating settlement: settlementId={}, amount={}, paidBy={}, paidTo={}", settlementid, request.getAmount(), request.getPaidby(), request.getPaidto());
        Settlement settlement = settlementRepository.findById(settlementid)
                .orElseThrow(() -> new SettlementNotFound("Settlement not found"));

        User paidBy = userRepository.findById(request.getPaidby())
                .orElseThrow(() -> new UserNotFound("PaidBy user not found"));

        User paidTo = userRepository.findById(request.getPaidto())
                .orElseThrow(() -> new UserNotFound("PaidTo user not found"));

        Long groupid = settlement.getGroupid().getGroupid();

        double netDue = expenseService.getBalanceBetweenUsers(groupid, paidBy.getUserid(), paidTo.getUserid());

        double adjustedDue = netDue + settlement.getAmount();

        if (request.getAmount() > adjustedDue){
            throw new SettleAmountMoreThanDue("Settlement amount exceeds dues. Due amount: " + adjustedDue);
        }

        settlement.setPaidby(paidBy);
        settlement.setPaidto(paidTo);
        settlement.setAmount(request.getAmount());
        settlement.setDate(Timestamp.valueOf(LocalDateTime.now()));

        Settlement saved = settlementRepository.save(settlement);
        log.info("Updated settlement: settlementId={}", saved.getId());
        return saved;
    }
}
