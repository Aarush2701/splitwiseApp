package splitwise.splitwise.service;

import java.util.List;

import splitwise.splitwise.dto.GroupDetailsResponse;
import splitwise.splitwise.model.ExpenseGroup;



public interface GroupService {

    // create group with initial members
    public ExpenseGroup createGroup(String groupname, List<Long> userIds);


    // get group details
    public GroupDetailsResponse getGroup(Long groupid);

    // add user to existing group
    public void addUserToGroup(Long groupid, Long userId);

    public void removeUserFromGroup(Long groupid, Long userId);

    public List<ExpenseGroup> getGroupByUserId(Long userId);

    public void deleteGroup(Long groupid);

}
