package splitwise.splitwise.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import splitwise.splitwise.dto.GroupDetailsResponse;
import splitwise.splitwise.dto.GroupMemberDTO;
import splitwise.splitwise.exception.*;
import splitwise.splitwise.model.*;
import splitwise.splitwise.repository.*;

import java.util.*;
import java.util.stream.Collectors;



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
