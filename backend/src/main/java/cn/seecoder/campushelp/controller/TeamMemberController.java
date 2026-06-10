package cn.seecoder.campushelp.controller;

import cn.seecoder.campushelp.common.ApiResult;
import cn.seecoder.campushelp.dto.ApplyTeamRequest;
import cn.seecoder.campushelp.dto.TeamMemberResponse;
import cn.seecoder.campushelp.service.TeamMemberService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/demands/{demandId}/team")
public class TeamMemberController {

    private final TeamMemberService teamMemberService;

    public TeamMemberController(TeamMemberService teamMemberService) {
        this.teamMemberService = teamMemberService;
    }

    /** Apply to join a team. */
    @PostMapping("/apply")
    public ApiResult<Void> apply(Authentication auth,
                                 @PathVariable Long demandId,
                                 @RequestBody(required = false) ApplyTeamRequest request) {
        Long userId = (Long) auth.getPrincipal();
        String message = request != null ? request.getMessage() : null;
        teamMemberService.apply(demandId, userId, message);
        return ApiResult.success();
    }

    /** Approve a pending applicant. */
    @PutMapping("/applicants/{applicantId}/approve")
    public ApiResult<Void> approve(Authentication auth,
                                   @PathVariable Long demandId,
                                   @PathVariable Long applicantId) {
        Long userId = (Long) auth.getPrincipal();
        teamMemberService.approve(demandId, applicantId, userId);
        return ApiResult.success();
    }

    /** Reject a pending applicant. */
    @PutMapping("/applicants/{applicantId}/reject")
    public ApiResult<Void> reject(Authentication auth,
                                  @PathVariable Long demandId,
                                  @PathVariable Long applicantId) {
        Long userId = (Long) auth.getPrincipal();
        teamMemberService.reject(demandId, applicantId, userId);
        return ApiResult.success();
    }

    /** Leave a team. */
    @PostMapping("/leave")
    public ApiResult<Void> leave(Authentication auth,
                                 @PathVariable Long demandId) {
        Long userId = (Long) auth.getPrincipal();
        teamMemberService.leave(demandId, userId);
        return ApiResult.success();
    }

    /** Remove a member (publisher only). */
    @DeleteMapping("/members/{memberId}")
    public ApiResult<Void> removeMember(Authentication auth,
                                        @PathVariable Long demandId,
                                        @PathVariable Long memberId) {
        Long userId = (Long) auth.getPrincipal();
        teamMemberService.removeMember(demandId, memberId, userId);
        return ApiResult.success();
    }

    /** Get joined members for a demand. */
    @GetMapping("/members")
    public ApiResult<List<TeamMemberResponse>> getMembers(@PathVariable Long demandId) {
        return ApiResult.success(teamMemberService.getJoinedMembers(demandId));
    }

    /** Get pending applicants (used by publisher, but we don't enforce auth here — service layer will). Callers check themselves. */
    @GetMapping("/applicants")
    public ApiResult<List<TeamMemberResponse>> getApplicants(Authentication auth,
                                                              @PathVariable Long demandId) {
        return ApiResult.success(teamMemberService.getPendingApplicants(demandId));
    }

    /** Get current user's membership status for this demand. */
    @GetMapping("/my-membership")
    public ApiResult<TeamMemberResponse> getMyMembership(Authentication auth,
                                                          @PathVariable Long demandId) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(teamMemberService.getMyMembership(demandId, userId));
    }
}
