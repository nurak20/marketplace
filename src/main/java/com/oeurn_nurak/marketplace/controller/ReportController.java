package com.oeurn_nurak.marketplace.controller;

import com.oeurn_nurak.marketplace.dto.response.ResponseDto;
import com.oeurn_nurak.marketplace.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Aggregated marketplace, engagement and payroll reports")
@PreAuthorize("hasAuthority('report:read')")
public class ReportController {

    private final ReportService reports;

    @GetMapping("/overview")
    @Operation(summary = "High-level platform KPIs")
    public ResponseDto<?> overview() {
        return ResponseDto.ok(reports.overview());
    }

    @GetMapping("/product-status")
    @Operation(summary = "Products grouped by listing status")
    public ResponseDto<?> productStatus() {
        return ResponseDto.ok(reports.productStatus());
    }

    @GetMapping("/categories")
    @Operation(summary = "Top categories with availability and value")
    public ResponseDto<?> categories() {
        return ResponseDto.ok(reports.categories());
    }

    @GetMapping("/monthly-activity")
    @Operation(summary = "New users, products, posts, comments and reactions per month")
    public ResponseDto<?> monthlyActivity(@RequestParam(defaultValue = "12") int months) {
        return ResponseDto.ok(reports.monthlyActivity(months));
    }

    @GetMapping("/engagement")
    @Operation(summary = "Comments, reactions, wishlists, reviews and messages per month")
    public ResponseDto<?> engagement(@RequestParam(defaultValue = "12") int months) {
        return ResponseDto.ok(reports.engagement(months));
    }

    @GetMapping("/top-sellers")
    @Operation(summary = "Sellers ranked by listings and sold revenue")
    public ResponseDto<?> topSellers(@RequestParam(defaultValue = "8") int limit) {
        return ResponseDto.ok(reports.topSellers(limit));
    }

    @GetMapping("/payroll/departments")
    @Operation(summary = "Latest approved payroll run broken down by department")
    public ResponseDto<?> payrollByDepartment() {
        return ResponseDto.ok(reports.payrollByDepartment());
    }

    @GetMapping("/payroll/runs")
    @Operation(summary = "Recent payroll runs with totals")
    public ResponseDto<?> payrollRuns(@RequestParam(defaultValue = "12") int limit) {
        return ResponseDto.ok(reports.payrollRuns(limit));
    }

    @GetMapping("/payroll/headcount")
    @Operation(summary = "Headcount per department by employment status")
    public ResponseDto<?> headcount() {
        return ResponseDto.ok(reports.headcount());
    }
}
