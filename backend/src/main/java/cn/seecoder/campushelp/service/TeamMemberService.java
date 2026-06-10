package cn.seecoder.campushelp.service;

import cn.seecoder.campushelp.dto.TeamMemberResponse;

import java.util.List;

public interface TeamMemberService {

    /** When a team demand is published, auto-add the publisher as LEADER with JOINED status. */
    void autoJoinLeader(Long demandId, Long publisherId);

    /** Apply to join a team demand. */
    void apply(Long demandId, Long userId, String message);

    /** Publisher approves a pending applicant. */
    void approve(Long demandId, Long applicantId, Long publisherId);

    /** Publisher rejects a pending applicant. */
    void reject(Long demandId, Long applicantId, Long publisherId);

    /** Member leaves a team. Leader cannot leave. */
    void leave(Long demandId, Long userId);

    /** Publisher removes a member. Cannot remove the leader. */
    void removeMember(Long demandId, Long memberId, Long publisherId);

    /** Get all joined members for a demand (including leader). */
    List<TeamMemberResponse> getJoinedMembers(Long demandId);

    /** Get all pending applicants for a demand. */
    List<TeamMemberResponse> getPendingApplicants(Long demandId);

    /** Get the current user's membership record for a demand (null if none). */
    TeamMemberResponse getMyMembership(Long demandId, Long userId);

    /** Count how many joined members a demand has. */
    int countJoined(Long demandId);

    /** Check if a user is a joined member of a demand. */
    boolean isJoined(Long demandId, Long userId);

    /** Check if a user has a pending application. */
    boolean hasPending(Long demandId, Long userId);

    /** Get all demand IDs where the user is a joined team member. */
    List<Long> getJoinedDemandIds(Long userId);
}
