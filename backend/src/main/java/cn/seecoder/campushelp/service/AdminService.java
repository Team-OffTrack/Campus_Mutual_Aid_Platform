package cn.seecoder.campushelp.service;

import cn.seecoder.campushelp.dto.AdminDashboardResponse;

/**
 * Admin dashboard and statistics service.
 */
public interface AdminService {

    /** Aggregate dashboard statistics. */
    AdminDashboardResponse getDashboard();
}
