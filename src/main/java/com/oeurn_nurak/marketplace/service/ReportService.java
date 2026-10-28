package com.oeurn_nurak.marketplace.service;

import com.oeurn_nurak.marketplace.dto.response.report.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-only reporting queries. Kept as hand-written SQL because the reports
 * combine aggregates across many tables that the JPA model does not expose.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final JdbcTemplate jdbc;

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public ReportOverview overview() {
        return jdbc.queryForObject("""
                SELECT
                    (SELECT count(*) FROM users WHERE deleted_at IS NULL) AS total_users,
                    (SELECT count(*) FROM users WHERE deleted_at IS NULL AND is_active) AS active_users,
                    (SELECT count(DISTINCT seller_id) FROM products WHERE deleted_at IS NULL) AS sellers,
                    (SELECT count(*) FROM products WHERE deleted_at IS NULL) AS products,
                    (SELECT count(*) FROM products WHERE deleted_at IS NULL AND status = 'AVAILABLE') AS available_products,
                    (SELECT count(*) FROM products WHERE deleted_at IS NULL AND status = 'SOLD') AS sold_products,
                    (SELECT count(*) FROM listing_posts WHERE deleted_at IS NULL) AS posts,
                    (SELECT count(*) FROM comments WHERE deleted_at IS NULL) AS comments,
                    (SELECT count(*) FROM post_reactions) AS reactions,
                    (SELECT count(*) FROM wishlists) AS wishlists,
                    (SELECT count(*) FROM employee_profiles) AS employees,
                    (SELECT count(*) FROM employee_profiles WHERE status = 'ACTIVE') AS active_employees,
                    (SELECT COALESCE(sum(total_gross), 0) FROM payroll_runs WHERE status = 'PAID') AS paid_gross,
                    (SELECT COALESCE(sum(total_net), 0) FROM payroll_runs WHERE status = 'PAID') AS paid_net,
                    (SELECT COALESCE(sum(price), 0) FROM products
                      WHERE deleted_at IS NULL AND status = 'AVAILABLE') AS available_value
                """, (rs, row) -> new ReportOverview(
                rs.getLong("total_users"),
                rs.getLong("active_users"),
                rs.getLong("sellers"),
                rs.getLong("products"),
                rs.getLong("available_products"),
                rs.getLong("sold_products"),
                rs.getLong("posts"),
                rs.getLong("comments"),
                rs.getLong("reactions"),
                rs.getLong("wishlists"),
                rs.getLong("employees"),
                rs.getLong("active_employees"),
                rs.getBigDecimal("paid_gross"),
                rs.getBigDecimal("paid_net"),
                rs.getBigDecimal("available_value")));
    }

    public List<ProductStatusReport> productStatus() {
        return jdbc.query("""
                SELECT status::text AS status,
                       count(*) AS products,
                       COALESCE(sum(price), 0) AS value
                FROM products
                WHERE deleted_at IS NULL
                GROUP BY status
                ORDER BY products DESC, status
                """, (rs, row) -> new ProductStatusReport(
                rs.getString("status"),
                rs.getLong("products"),
                rs.getBigDecimal("value")));
    }

    public List<CategoryReport> categories() {
        return jdbc.query("""
                SELECT COALESCE(NULLIF(trim(category), ''), 'Uncategorized') AS category,
                       count(*) AS products,
                       count(*) FILTER (WHERE status = 'AVAILABLE') AS available,
                       count(*) FILTER (WHERE status = 'SOLD') AS sold,
                       count(*) FILTER (WHERE status = 'RESERVED') AS reserved,
                       COALESCE(sum(price), 0) AS value
                FROM products
                WHERE deleted_at IS NULL
                GROUP BY 1
                ORDER BY products DESC, category
                LIMIT 12
                """, (rs, row) -> new CategoryReport(
                rs.getString("category"),
                rs.getLong("products"),
                rs.getLong("available"),
                rs.getLong("sold"),
                rs.getLong("reserved"),
                rs.getBigDecimal("value")));
    }

    public List<MonthlyActivityReport> monthlyActivity(int months) {
        int span = clamp(months, 1, 24) - 1;
        return jdbc.query("""
                WITH months AS (
                    SELECT date_trunc('month', CURRENT_DATE) - make_interval(months => n) AS month_start
                    FROM generate_series(0, ?) AS n
                )
                SELECT to_char(m.month_start, 'YYYY-MM') AS month,
                       (SELECT count(*) FROM users u
                         WHERE u.deleted_at IS NULL
                           AND u.created_at >= m.month_start
                           AND u.created_at < m.month_start + INTERVAL '1 month') AS users,
                       (SELECT count(*) FROM products p
                         WHERE p.deleted_at IS NULL
                           AND p.created_at >= m.month_start
                           AND p.created_at < m.month_start + INTERVAL '1 month') AS products,
                       (SELECT count(*) FROM listing_posts lp
                         WHERE lp.deleted_at IS NULL
                           AND lp.created_at >= m.month_start
                           AND lp.created_at < m.month_start + INTERVAL '1 month') AS posts,
                       (SELECT count(*) FROM comments c
                         WHERE c.deleted_at IS NULL
                           AND c.created_at >= m.month_start
                           AND c.created_at < m.month_start + INTERVAL '1 month') AS comments,
                       (SELECT count(*) FROM post_reactions r
                         WHERE r.created_at >= m.month_start
                           AND r.created_at < m.month_start + INTERVAL '1 month') AS reactions
                FROM months m
                ORDER BY m.month_start
                """, (rs, row) -> new MonthlyActivityReport(
                rs.getString("month"),
                rs.getLong("users"),
                rs.getLong("products"),
                rs.getLong("posts"),
                rs.getLong("comments"),
                rs.getLong("reactions")), span);
    }

    public List<EngagementReport> engagement(int months) {
        int span = clamp(months, 1, 24) - 1;
        return jdbc.query("""
                WITH months AS (
                    SELECT date_trunc('month', CURRENT_DATE) - make_interval(months => n) AS month_start
                    FROM generate_series(0, ?) AS n
                )
                SELECT to_char(m.month_start, 'YYYY-MM') AS month,
                       (SELECT count(*) FROM comments c
                         WHERE c.deleted_at IS NULL
                           AND c.created_at >= m.month_start
                           AND c.created_at < m.month_start + INTERVAL '1 month') AS comments,
                       (SELECT count(*) FROM post_reactions r
                         WHERE r.created_at >= m.month_start
                           AND r.created_at < m.month_start + INTERVAL '1 month') AS reactions,
                       (SELECT count(*) FROM wishlists w
                         WHERE w.created_at >= m.month_start
                           AND w.created_at < m.month_start + INTERVAL '1 month') AS wishlists,
                       (SELECT count(*) FROM reviews rv
                         WHERE rv.created_at >= m.month_start
                           AND rv.created_at < m.month_start + INTERVAL '1 month') AS reviews,
                       (SELECT count(*) FROM messages msg
                         WHERE msg.created_at >= m.month_start
                           AND msg.created_at < m.month_start + INTERVAL '1 month') AS messages
                FROM months m
                ORDER BY m.month_start
                """, (rs, row) -> new EngagementReport(
                rs.getString("month"),
                rs.getLong("comments"),
                rs.getLong("reactions"),
                rs.getLong("wishlists"),
                rs.getLong("reviews"),
                rs.getLong("messages")), span);
    }

    public List<TopSellerReport> topSellers(int limit) {
        return jdbc.query("""
                SELECT u.id AS user_id,
                       COALESCE(NULLIF(trim(u.full_name), ''), u.username) AS name,
                       count(p.id) AS listings,
                       count(p.id) FILTER (WHERE p.status = 'SOLD') AS sold,
                       COALESCE(sum(p.price) FILTER (WHERE p.status = 'SOLD'), 0) AS revenue,
                       COALESCE((SELECT count(*)
                                   FROM post_reactions r
                                   JOIN listing_posts lp ON lp.id = r.post_id
                                  WHERE lp.user_id = u.id AND lp.deleted_at IS NULL), 0) AS reactions,
                       COALESCE((SELECT count(*)
                                   FROM comments c
                                   JOIN listing_posts lp ON lp.id = c.post_id
                                  WHERE lp.user_id = u.id
                                    AND lp.deleted_at IS NULL
                                    AND c.deleted_at IS NULL), 0) AS comments
                FROM users u
                JOIN products p ON p.seller_id = u.id AND p.deleted_at IS NULL
                WHERE u.deleted_at IS NULL
                GROUP BY u.id, u.full_name, u.username
                ORDER BY listings DESC, revenue DESC, u.id
                LIMIT ?
                """, (rs, row) -> new TopSellerReport(
                rs.getLong("user_id"),
                rs.getString("name"),
                rs.getLong("listings"),
                rs.getLong("sold"),
                rs.getBigDecimal("revenue"),
                rs.getLong("reactions"),
                rs.getLong("comments")), clamp(limit, 1, 50));
    }

    public List<PayrollDepartmentReport> payrollByDepartment() {
        return jdbc.query("""
                WITH latest AS (
                    SELECT id, period_year, period_month
                    FROM payroll_runs
                    WHERE status IN ('APPROVED', 'PAID')
                    ORDER BY period_year DESC, period_month DESC
                    LIMIT 1
                )
                SELECT l.period_year,
                       l.period_month,
                       COALESCE(NULLIF(trim(ps.department), ''), 'Unassigned') AS department,
                       count(*) AS employees,
                       COALESCE(sum(ps.gross_pay), 0) AS gross,
                       COALESCE(sum(ps.deductions_total), 0) AS deductions,
                       COALESCE(sum(ps.net_pay), 0) AS net
                FROM payslips ps
                JOIN latest l ON l.id = ps.run_id
                GROUP BY l.period_year, l.period_month, 3
                ORDER BY net DESC, department
                """, (rs, row) -> new PayrollDepartmentReport(
                rs.getInt("period_year"),
                rs.getInt("period_month"),
                rs.getString("department"),
                rs.getLong("employees"),
                rs.getBigDecimal("gross"),
                rs.getBigDecimal("deductions"),
                rs.getBigDecimal("net")));
    }

    public List<PayrollRunReport> payrollRuns(int limit) {
        return jdbc.query("""
                SELECT r.period_year,
                       r.period_month,
                       r.status::text AS status,
                       (SELECT count(*) FROM payslips ps WHERE ps.run_id = r.id) AS employees,
                       r.total_gross,
                       r.total_deductions,
                       r.total_net
                FROM payroll_runs r
                ORDER BY r.period_year DESC, r.period_month DESC
                LIMIT ?
                """, (rs, row) -> new PayrollRunReport(
                rs.getInt("period_year"),
                rs.getInt("period_month"),
                rs.getString("status"),
                rs.getLong("employees"),
                rs.getBigDecimal("total_gross"),
                rs.getBigDecimal("total_deductions"),
                rs.getBigDecimal("total_net")), clamp(limit, 1, 36));
    }

    public List<HeadcountReport> headcount() {
        return jdbc.query("""
                SELECT COALESCE(NULLIF(trim(department), ''), 'Unassigned') AS department,
                       count(*) FILTER (WHERE status = 'ACTIVE') AS active,
                       count(*) FILTER (WHERE status = 'ON_LEAVE') AS on_leave,
                       count(*) FILTER (WHERE status = 'TERMINATED') AS terminated,
                       count(*) AS total
                FROM employee_profiles
                GROUP BY 1
                ORDER BY total DESC, department
                """, (rs, row) -> new HeadcountReport(
                rs.getString("department"),
                rs.getLong("active"),
                rs.getLong("on_leave"),
                rs.getLong("terminated"),
                rs.getLong("total")));
    }
}
