package cn.seecoder.campushelp.service.impl;

import cn.seecoder.campushelp.common.BusinessException;
import cn.seecoder.campushelp.common.ResultCode;
import cn.seecoder.campushelp.dto.TeamMemberResponse;
import cn.seecoder.campushelp.entity.Demand;
import cn.seecoder.campushelp.entity.TeamMember;
import cn.seecoder.campushelp.entity.User;
import cn.seecoder.campushelp.entity.enums.DemandStatus;
import cn.seecoder.campushelp.entity.enums.NotificationType;
import cn.seecoder.campushelp.entity.enums.TeamMemberRole;
import cn.seecoder.campushelp.entity.enums.TeamMemberStatus;
import cn.seecoder.campushelp.mapper.DemandMapper;
import cn.seecoder.campushelp.mapper.TeamMemberMapper;
import cn.seecoder.campushelp.mapper.UserMapper;
import cn.seecoder.campushelp.service.NotificationService;
import cn.seecoder.campushelp.service.TeamMemberService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class TeamMemberServiceImpl implements TeamMemberService {

    private final TeamMemberMapper teamMemberMapper;
    private final DemandMapper demandMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public TeamMemberServiceImpl(TeamMemberMapper teamMemberMapper, DemandMapper demandMapper,
                                  UserMapper userMapper, NotificationService notificationService) {
        this.teamMemberMapper = teamMemberMapper;
        this.demandMapper = demandMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public void autoJoinLeader(Long demandId, Long publisherId) {
        TeamMember leader = new TeamMember();
        leader.setDemandId(demandId);
        leader.setUserId(publisherId);
        leader.setRole(TeamMemberRole.LEADER);
        leader.setStatus(TeamMemberStatus.JOINED);
        teamMemberMapper.insert(leader);
    }

    @Override
    @Transactional
    public void apply(Long demandId, Long userId, String message) {
        Demand demand = findDemandOrFail(demandId);

        if (!DemandStatus.OPEN.equals(demand.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该队伍已停止招募");
        }
        if (demand.getPublisherId().equals(userId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能申请加入自己的队伍");
        }

        TeamMember existing = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getDemandId, demandId)
                        .eq(TeamMember::getUserId, userId));
        if (existing != null) {
            if (TeamMemberStatus.PENDING.equals(existing.getStatus())) {
                throw new BusinessException(ResultCode.CONFLICT, "已提交过申请，请等待队长审核");
            }
            if (TeamMemberStatus.JOINED.equals(existing.getStatus())) {
                throw new BusinessException(ResultCode.CONFLICT, "您已是该队伍成员");
            }
            if (TeamMemberStatus.REJECTED.equals(existing.getStatus())) {
                // Allow re-apply by removing old rejection
                teamMemberMapper.deleteById(existing.getId());
            }
        }

        int teamSize = getTeamSizeFromAttributes(demand);
        int joinedCount = countJoined(demandId);
        if (joinedCount >= teamSize) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "队伍已满员");
        }

        TeamMember member = new TeamMember();
        member.setDemandId(demandId);
        member.setUserId(userId);
        member.setRole(TeamMemberRole.MEMBER);
        member.setStatus(TeamMemberStatus.PENDING);
        member.setMessage(message);
        teamMemberMapper.insert(member);

        User applicant = userMapper.selectById(userId);
        String applicantName = applicant != null ? applicant.getName() : "未知用户";
        notificationService.create(
                demand.getPublisherId(),
                NotificationType.JOIN_REQUEST,
                "新队员申请",
                applicantName + " 申请加入你的队伍「" + demand.getTitle() + "」",
                demandId);
    }

    @Override
    @Transactional
    public void approve(Long demandId, Long applicantId, Long publisherId) {
        Demand demand = findDemandOrFail(demandId);
        if (!demand.getPublisherId().equals(publisherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有队长可以审批申请");
        }

        TeamMember member = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getDemandId, demandId)
                        .eq(TeamMember::getUserId, applicantId)
                        .eq(TeamMember::getStatus, TeamMemberStatus.PENDING));
        if (member == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "未找到该申请");
        }

        int teamSize = getTeamSizeFromAttributes(demand);
        if (countJoined(demandId) >= teamSize) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "队伍已满员，无法通过更多申请");
        }

        member.setStatus(TeamMemberStatus.JOINED);
        teamMemberMapper.updateById(member);

        notificationService.create(
                applicantId,
                NotificationType.REQUEST_APPROVED,
                "申请已通过",
                "你已成功加入队伍「" + demand.getTitle() + "」",
                demandId);
    }

    @Override
    @Transactional
    public void reject(Long demandId, Long applicantId, Long publisherId) {
        Demand demand = findDemandOrFail(demandId);
        if (!demand.getPublisherId().equals(publisherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有队长可以审批申请");
        }

        TeamMember member = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getDemandId, demandId)
                        .eq(TeamMember::getUserId, applicantId)
                        .eq(TeamMember::getStatus, TeamMemberStatus.PENDING));
        if (member == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "未找到该申请");
        }

        member.setStatus(TeamMemberStatus.REJECTED);
        teamMemberMapper.updateById(member);

        notificationService.create(
                applicantId,
                NotificationType.REQUEST_REJECTED,
                "申请未通过",
                "你申请加入队伍「" + demand.getTitle() + "」的请求已被队长拒绝",
                demandId);
    }

    @Override
    @Transactional
    public void leave(Long demandId, Long userId) {
        TeamMember member = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getDemandId, demandId)
                        .eq(TeamMember::getUserId, userId)
                        .eq(TeamMember::getStatus, TeamMemberStatus.JOINED));
        if (member == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "你不在该队伍中");
        }
        if (TeamMemberRole.LEADER.equals(member.getRole())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "队长不能退出队伍，请解散队伍或转让队长");
        }

        teamMemberMapper.deleteById(member.getId());
    }

    @Override
    @Transactional
    public void removeMember(Long demandId, Long memberId, Long publisherId) {
        Demand demand = findDemandOrFail(demandId);
        if (!demand.getPublisherId().equals(publisherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有队长可以移除成员");
        }

        TeamMember member = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getDemandId, demandId)
                        .eq(TeamMember::getUserId, memberId)
                        .eq(TeamMember::getStatus, TeamMemberStatus.JOINED));
        if (member == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该用户不在队伍中");
        }
        if (TeamMemberRole.LEADER.equals(member.getRole())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能移除队长");
        }

        teamMemberMapper.deleteById(member.getId());
    }

    @Override
    public List<TeamMemberResponse> getJoinedMembers(Long demandId) {
        List<TeamMember> members = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getDemandId, demandId)
                        .eq(TeamMember::getStatus, TeamMemberStatus.JOINED)
                        .orderByAsc(TeamMember::getCreateTime));
        return members.stream()
                .map(m -> TeamMemberResponse.from(m, userMapper.selectById(m.getUserId())))
                .toList();
    }

    @Override
    public List<TeamMemberResponse> getPendingApplicants(Long demandId) {
        List<TeamMember> applicants = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getDemandId, demandId)
                        .eq(TeamMember::getStatus, TeamMemberStatus.PENDING)
                        .orderByAsc(TeamMember::getCreateTime));
        return applicants.stream()
                .map(a -> TeamMemberResponse.from(a, userMapper.selectById(a.getUserId())))
                .toList();
    }

    @Override
    public TeamMemberResponse getMyMembership(Long demandId, Long userId) {
        TeamMember member = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getDemandId, demandId)
                        .eq(TeamMember::getUserId, userId));
        if (member == null) return null;
        return TeamMemberResponse.from(member, userMapper.selectById(userId));
    }

    @Override
    public int countJoined(Long demandId) {
        return Math.toIntExact(teamMemberMapper.selectCount(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getDemandId, demandId)
                        .eq(TeamMember::getStatus, TeamMemberStatus.JOINED)));
    }

    @Override
    public boolean isJoined(Long demandId, Long userId) {
        return teamMemberMapper.selectCount(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getDemandId, demandId)
                        .eq(TeamMember::getUserId, userId)
                        .eq(TeamMember::getStatus, TeamMemberStatus.JOINED)) > 0;
    }

    @Override
    public boolean hasPending(Long demandId, Long userId) {
        return teamMemberMapper.selectCount(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getDemandId, demandId)
                        .eq(TeamMember::getUserId, userId)
                        .eq(TeamMember::getStatus, TeamMemberStatus.PENDING)) > 0;
    }

    @Override
    public List<Long> getJoinedDemandIds(Long userId) {
        List<TeamMember> members = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getUserId, userId)
                        .eq(TeamMember::getStatus, TeamMemberStatus.JOINED));
        return members.stream().map(TeamMember::getDemandId).toList();
    }

    private Demand findDemandOrFail(Long demandId) {
        Demand demand = demandMapper.selectById(demandId);
        if (demand == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "需求不存在");
        }
        return demand;
    }

    private int getTeamSizeFromAttributes(Demand demand) {
        if (demand.getAttributes() == null || demand.getAttributes().isBlank()) {
            return 2;
        }
        try {
            Map<String, Object> attrs = OBJECT_MAPPER.readValue(
                    demand.getAttributes(), new TypeReference<Map<String, Object>>() {});
            Object size = attrs.get("team_size");
            if (size instanceof Number n) {
                return n.intValue();
            }
        } catch (Exception ignored) {
        }
        return 2;
    }
}
