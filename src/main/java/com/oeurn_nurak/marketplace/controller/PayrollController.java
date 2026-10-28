package com.oeurn_nurak.marketplace.controller;

import com.oeurn_nurak.marketplace.dto.request.*;
import com.oeurn_nurak.marketplace.dto.response.ResponseDto;
import com.oeurn_nurak.marketplace.entity.enums.EmploymentStatus;
import com.oeurn_nurak.marketplace.service.PayrollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payroll;

    @GetMapping("/employees")
    @PreAuthorize("hasAuthority('payroll:read')")
    public ResponseDto<?> employees(@RequestParam(required = false) EmploymentStatus status,
                                    @RequestParam(required = false) String department,
                                    @RequestParam(required = false, name = "q") String q,
                                    Pageable page) {
        return ResponseDto.ok(payroll.listEmployees(status, department, q, page));
    }

    @GetMapping("/employees/{userId}")
    @PreAuthorize("hasAuthority('payroll:read')")
    public ResponseDto<?> employee(@PathVariable Long userId) {
        return ResponseDto.ok(payroll.employee(userId));
    }

    @PostMapping("/employees")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('payroll:manage')")
    public ResponseDto<?> enroll(@Valid @RequestBody EnrollEmployeeRequest request) {
        return ResponseDto.created("Employee enrolled", payroll.enroll(request));
    }

    @PutMapping("/employees/{userId}")
    @PreAuthorize("hasAuthority('payroll:manage')")
    public ResponseDto<?> updateEmployee(@PathVariable Long userId,
                                         @Valid @RequestBody UpdateEmployeeRequest request) {
        return ResponseDto.ok(payroll.updateEmployee(userId, request));
    }

    @PatchMapping("/employees/{userId}/status")
    @PreAuthorize("hasAuthority('payroll:manage')")
    public ResponseDto<?> updateEmployeeStatus(@PathVariable Long userId,
                                               @Valid @RequestBody UpdateEmploymentStatusRequest request) {
        return ResponseDto.ok(payroll.setEmployeeStatus(userId, request.status()));
    }

    @GetMapping("/runs")
    @PreAuthorize("hasAuthority('payroll:read')")
    public ResponseDto<?> runs(Pageable page) {
        return ResponseDto.ok(payroll.listRuns(page));
    }

    @GetMapping("/runs/{id}")
    @PreAuthorize("hasAuthority('payroll:read')")
    public ResponseDto<?> run(@PathVariable Long id) {
        return ResponseDto.ok(payroll.run(id));
    }

    @PostMapping("/runs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('payroll:manage')")
    public ResponseDto<?> createRun(@Valid @RequestBody CreatePayrollRunRequest request) {
        return ResponseDto.created("Payroll run created", payroll.createRun(request));
    }

    @PostMapping("/runs/{id}/regenerate")
    @PreAuthorize("hasAuthority('payroll:manage')")
    public ResponseDto<?> regenerate(@PathVariable Long id) {
        return ResponseDto.ok("Payroll run regenerated", payroll.regenerate(id));
    }

    @GetMapping("/runs/{id}/payslips")
    @PreAuthorize("hasAuthority('payroll:read')")
    public ResponseDto<?> runPayslips(@PathVariable Long id, Pageable page) {
        return ResponseDto.ok(payroll.runPayslips(id, page));
    }

    @GetMapping("/payslips/{id}")
    @PreAuthorize("hasAuthority('payroll:read')")
    public ResponseDto<?> payslip(@PathVariable Long id) {
        return ResponseDto.ok(payroll.payslip(id));
    }

    @PatchMapping("/payslips/{id}")
    @PreAuthorize("hasAuthority('payroll:manage')")
    public ResponseDto<?> updatePayslip(@PathVariable Long id,
                                        @Valid @RequestBody UpdatePayslipRequest request) {
        return ResponseDto.ok(payroll.updatePayslip(id, request));
    }

    @PostMapping("/runs/{id}/approve")
    @PreAuthorize("hasAuthority('payroll:approve')")
    public ResponseDto<?> approve(@PathVariable Long id) {
        return ResponseDto.ok("Payroll run approved", payroll.approve(id));
    }

    @PostMapping("/runs/{id}/pay")
    @PreAuthorize("hasAuthority('payroll:approve')")
    public ResponseDto<?> pay(@PathVariable Long id) {
        return ResponseDto.ok("Payroll run marked paid", payroll.pay(id));
    }

    @PostMapping("/runs/{id}/cancel")
    @PreAuthorize("hasAuthority('payroll:manage')")
    public ResponseDto<?> cancel(@PathVariable Long id) {
        return ResponseDto.ok("Payroll run cancelled", payroll.cancel(id));
    }

    @GetMapping("/me/profile")
    public ResponseDto<?> myProfile() {
        return ResponseDto.ok(payroll.myProfile());
    }

    @GetMapping("/me/payslips")
    public ResponseDto<?> myPayslips(Pageable page) {
        return ResponseDto.ok(payroll.myPayslips(page));
    }

    @GetMapping("/me/payslips/{id}")
    public ResponseDto<?> myPayslip(@PathVariable Long id) {
        return ResponseDto.ok(payroll.myPayslip(id));
    }
}
