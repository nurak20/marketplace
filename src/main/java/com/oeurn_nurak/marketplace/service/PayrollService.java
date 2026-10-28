package com.oeurn_nurak.marketplace.service;

import com.oeurn_nurak.marketplace.dto.request.*;
import com.oeurn_nurak.marketplace.dto.response.*;
import com.oeurn_nurak.marketplace.entity.*;
import com.oeurn_nurak.marketplace.entity.enums.*;
import com.oeurn_nurak.marketplace.exception.*;
import com.oeurn_nurak.marketplace.repository.*;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PayrollService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final List<EmploymentStatus> PAYABLE_STATUSES =
            List.of(EmploymentStatus.ACTIVE, EmploymentStatus.ON_LEAVE);
    private static final List<PayrollRunStatus> EMPLOYEE_VISIBLE_STATUSES =
            List.of(PayrollRunStatus.APPROVED, PayrollRunStatus.PAID);

    private final EmployeeProfileRepository employees;
    private final PayrollRunRepository runs;
    private final PayslipRepository payslips;
    private final UserRepository users;
    private final NotificationService notifications;
    private final CurrentUser current;

    @Transactional(readOnly = true)
    public PageDto<EmployeeProfileResponse> listEmployees(EmploymentStatus status, String department,
                                                          String q, Pageable pageable) {
        Page<EmployeeProfile> page = employees.findAll(employeeSpec(status, department, q), pageable);
        Map<Long, User> userMap = page.getContent().isEmpty() ? Map.of()
                : users.findAllById(page.getContent().stream().map(EmployeeProfile::getUserId).toList())
                    .stream().collect(Collectors.toMap(User::getId, user -> user));
        return PageDto.from(page.map(profile ->
                EmployeeProfileResponse.from(profile, userMap.get(profile.getUserId()))));
    }

    @Transactional(readOnly = true)
    public EmployeeProfileResponse employee(Long userId) {
        return EmployeeProfileResponse.from(requireProfile(userId), users.findById(userId).orElse(null));
    }

    public EmployeeProfileResponse enroll(EnrollEmployeeRequest request) {
        User user = requireUser(request.userId());
        if (!Boolean.TRUE.equals(user.getIsActive()))
            throw new BadRequestException("Inactive users cannot be enrolled in payroll");
        if (employees.existsByUserId(user.getId()))
            throw new ConflictException("User is already enrolled in payroll");
        String code = request.employeeCode() == null || request.employeeCode().isBlank()
                ? String.format("EMP-%04d", user.getId())
                : request.employeeCode().trim();
        if (employees.existsByEmployeeCode(code))
            throw new ConflictException("Employee code already exists");
        EmployeeProfile profile = EmployeeProfile.builder()
                .userId(user.getId())
                .employeeCode(code)
                .jobTitle(trimToNull(request.jobTitle()))
                .department(trimToNull(request.department()))
                .baseSalary(money(request.baseSalary()))
                .allowance(money(request.allowance()))
                .currency(normalizeCurrency(request.currency()))
                .hireDate(request.hireDate())
                .bankName(trimToNull(request.bankName()))
                .bankAccount(trimToNull(request.bankAccount()))
                .status(EmploymentStatus.ACTIVE)
                .build();
        return EmployeeProfileResponse.from(employees.save(profile), user);
    }

    public EmployeeProfileResponse updateEmployee(Long userId, UpdateEmployeeRequest request) {
        EmployeeProfile profile = requireProfile(userId);
        User user = requireUser(userId);
        if (request.jobTitle() != null) profile.setJobTitle(trimToNull(request.jobTitle()));
        if (request.department() != null) profile.setDepartment(trimToNull(request.department()));
        profile.setBaseSalary(money(request.baseSalary()));
        if (request.allowance() != null) profile.setAllowance(money(request.allowance()));
        if (request.currency() != null) profile.setCurrency(normalizeCurrency(request.currency()));
        if (request.hireDate() != null) profile.setHireDate(request.hireDate());
        if (request.bankName() != null) profile.setBankName(trimToNull(request.bankName()));
        if (request.bankAccount() != null) profile.setBankAccount(trimToNull(request.bankAccount()));
        return EmployeeProfileResponse.from(employees.save(profile), user);
    }

    public EmployeeProfileResponse setEmployeeStatus(Long userId, EmploymentStatus status) {
        EmployeeProfile profile = requireProfile(userId);
        profile.setStatus(status);
        return EmployeeProfileResponse.from(employees.save(profile), requireUser(userId));
    }

    @Transactional(readOnly = true)
    public PageDto<PayrollRunResponse> listRuns(Pageable pageable) {
        return PageDto.from(runs.findAll(pageable).map(PayrollRunResponse::from));
    }

    @Transactional(readOnly = true)
    public PayrollRunResponse run(Long id) {
        return PayrollRunResponse.from(requireRun(id));
    }

    public PayrollRunResponse createRun(CreatePayrollRunRequest request) {
        LocalDate today = LocalDate.now();
        if (request.year() > today.getYear()
                || (request.year().equals(today.getYear()) && request.month() > today.getMonthValue()))
            throw new BadRequestException("Payroll runs cannot be created for a future period");
        if (runs.existsByPeriodYearAndPeriodMonth(request.year(), request.month()))
            throw new ConflictException("A payroll run already exists for that period");
        String currency = normalizeCurrency(request.currency());
        List<EmployeeProfile> profiles = eligibleProfiles(request.year(), request.month(), currency);
        if (profiles.isEmpty())
            throw new BadRequestException("No eligible employees for this payroll run");
        PayrollRun run = PayrollRun.builder()
                .periodYear(request.year())
                .periodMonth(request.month())
                .currency(currency)
                .notes(trimToNull(request.notes()))
                .createdBy(current.id())
                .build();
        runs.save(run);
        generatePayslips(run, profiles);
        return PayrollRunResponse.from(run);
    }

    public PayrollRunResponse regenerate(Long id) {
        PayrollRun run = requireRun(id);
        requireDraft(run, "regenerated");
        payslips.deleteAllByRunId(id);
        List<EmployeeProfile> profiles =
                eligibleProfiles(run.getPeriodYear(), run.getPeriodMonth(), run.getCurrency());
        if (profiles.isEmpty())
            throw new BadRequestException("No eligible employees for this payroll run");
        generatePayslips(run, profiles);
        return PayrollRunResponse.from(run);
    }

    @Transactional(readOnly = true)
    public PageDto<PayslipResponse> runPayslips(Long runId, Pageable pageable) {
        PayrollRun run = requireRun(runId);
        return PageDto.from(payslips.findByRunId(runId, pageable)
                .map(payslip -> PayslipResponse.from(payslip, run)));
    }

    @Transactional(readOnly = true)
    public PayslipResponse payslip(Long id) {
        Payslip payslip = payslips.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip", id));
        return PayslipResponse.from(payslip, requireRun(payslip.getRunId()));
    }

    public PayslipResponse updatePayslip(Long id, UpdatePayslipRequest request) {
        Payslip payslip = payslips.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip", id));
        PayrollRun run = requireRun(payslip.getRunId());
        requireDraft(run, "edited");
        if (request.bonus() != null) payslip.setBonus(money(request.bonus()));
        if (request.overtime() != null) payslip.setOvertime(money(request.overtime()));
        if (request.tax() != null) payslip.setTax(money(request.tax()));
        if (request.otherDeductions() != null) payslip.setOtherDeductions(money(request.otherDeductions()));
        if (request.notes() != null) payslip.setNotes(trimToNull(request.notes()));
        BigDecimal gross = money(payslip.getBaseSalary().add(payslip.getAllowance())
                .add(payslip.getBonus()).add(payslip.getOvertime()));
        BigDecimal deductions = money(payslip.getTax().add(payslip.getOtherDeductions()));
        if (gross.compareTo(deductions) < 0)
            throw new BadRequestException("Deductions cannot exceed gross pay");
        payslip.setGrossPay(gross);
        payslip.setDeductionsTotal(deductions);
        payslip.setNetPay(money(gross.subtract(deductions)));
        payslips.save(payslip);
        recalculateTotals(run, payslips.findByRunIdOrderByIdAsc(run.getId()));
        runs.save(run);
        return PayslipResponse.from(payslip, run);
    }

    public PayrollRunResponse approve(Long id) {
        PayrollRun run = requireRun(id);
        if (run.getStatus() != PayrollRunStatus.DRAFT)
            throw new ConflictException("Only draft payroll runs can be approved");
        if (payslips.countByRunId(id) == 0)
            throw new BadRequestException("A payroll run must have at least one payslip");
        run.setStatus(PayrollRunStatus.APPROVED);
        run.setApprovedBy(current.id());
        run.setApprovedAt(LocalDateTime.now());
        return PayrollRunResponse.from(runs.save(run));
    }

    public PayrollRunResponse pay(Long id) {
        PayrollRun run = requireRun(id);
        if (run.getStatus() != PayrollRunStatus.APPROVED)
            throw new ConflictException("Only approved payroll runs can be marked paid");
        run.setStatus(PayrollRunStatus.PAID);
        run.setPaidAt(LocalDateTime.now());
        runs.save(run);
        String period = Month.of(run.getPeriodMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " " + run.getPeriodYear();
        payslips.findByRunIdOrderByIdAsc(id).forEach(payslip ->
                notifications.send(payslip.getUserId(), current.id(), NotifType.PAYROLL,
                        "Your payslip for " + period + " is available", payslip.getId()));
        return PayrollRunResponse.from(run);
    }

    public PayrollRunResponse cancel(Long id) {
        PayrollRun run = requireRun(id);
        if (run.getStatus() != PayrollRunStatus.DRAFT)
            throw new ConflictException("Only draft payroll runs can be cancelled");
        run.setStatus(PayrollRunStatus.CANCELLED);
        return PayrollRunResponse.from(runs.save(run));
    }

    @Transactional(readOnly = true)
    public EmployeeProfileResponse myProfile() {
        EmployeeProfile profile = employees.findByUserId(current.id())
                .orElseThrow(() -> new ResourceNotFoundException("You are not enrolled in payroll"));
        return EmployeeProfileResponse.from(profile, current.get());
    }

    @Transactional(readOnly = true)
    public PageDto<PayslipResponse> myPayslips(Pageable pageable) {
        Page<Payslip> page = payslips.findVisibleByUserId(current.id(), EMPLOYEE_VISIBLE_STATUSES, pageable);
        Map<Long, PayrollRun> runMap = page.getContent().isEmpty() ? Map.of()
                : runs.findAllById(page.getContent().stream().map(Payslip::getRunId).distinct().toList())
                    .stream().collect(Collectors.toMap(PayrollRun::getId, run -> run));
        return PageDto.from(page.map(payslip ->
                PayslipResponse.from(payslip, runMap.get(payslip.getRunId()))));
    }

    @Transactional(readOnly = true)
    public PayslipResponse myPayslip(Long id) {
        Payslip payslip = payslips.findByIdAndUserId(id, current.id())
                .orElseThrow(() -> new ResourceNotFoundException("Payslip", id));
        PayrollRun run = requireRun(payslip.getRunId());
        if (!EMPLOYEE_VISIBLE_STATUSES.contains(run.getStatus()))
            throw new ResourceNotFoundException("Payslip", id);
        return PayslipResponse.from(payslip, run);
    }

    private Specification<EmployeeProfile> employeeSpec(EmploymentStatus status, String department, String q) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (department != null && !department.isBlank())
                predicates.add(cb.equal(cb.lower(root.get("department")),
                        department.trim().toLowerCase(Locale.ROOT)));
            if (q != null && !q.isBlank()) {
                String pattern = likePattern(q);
                Root<User> user = query.from(User.class);
                predicates.add(cb.equal(user.get("id"), root.get("userId")));
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("employeeCode")), pattern, '\\'),
                        cb.like(cb.lower(user.get("username")), pattern, '\\'),
                        cb.like(cb.lower(user.get("fullName")), pattern, '\\')));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private List<EmployeeProfile> eligibleProfiles(int year, int month, String currency) {
        LocalDate periodEnd = YearMonth.of(year, month).atEndOfMonth();
        List<EmployeeProfile> eligible = employees.findByStatusIn(PAYABLE_STATUSES).stream()
                .filter(profile -> profile.getHireDate() == null
                        || !profile.getHireDate().isAfter(periodEnd))
                .toList();
        List<EmployeeProfile> mismatched = eligible.stream()
                .filter(profile -> !profile.getCurrency().equalsIgnoreCase(currency))
                .toList();
        if (!mismatched.isEmpty())
            throw new BadRequestException("Employee profiles use a different currency: "
                    + mismatched.stream().map(EmployeeProfile::getEmployeeCode).limit(5)
                        .collect(Collectors.joining(", ")));
        return eligible;
    }

    private void generatePayslips(PayrollRun run, List<EmployeeProfile> profiles) {
        Map<Long, User> userMap = users.findAllById(
                profiles.stream().map(EmployeeProfile::getUserId).toList())
                .stream().collect(Collectors.toMap(User::getId, user -> user));
        List<Payslip> generated = profiles.stream().map(profile -> {
            BigDecimal gross = money(profile.getBaseSalary().add(profile.getAllowance()));
            return Payslip.builder()
                    .runId(run.getId())
                    .userId(profile.getUserId())
                    .employeeCode(profile.getEmployeeCode())
                    .employeeName(employeeName(profile, userMap.get(profile.getUserId())))
                    .jobTitle(profile.getJobTitle())
                    .department(profile.getDepartment())
                    .baseSalary(money(profile.getBaseSalary()))
                    .allowance(money(profile.getAllowance()))
                    .grossPay(gross)
                    .netPay(gross)
                    .currency(profile.getCurrency())
                    .build();
        }).toList();
        List<Payslip> saved = payslips.saveAll(generated);
        recalculateTotals(run, saved);
        runs.save(run);
    }

    private void recalculateTotals(PayrollRun run, List<Payslip> items) {
        run.setTotalGross(sum(items, Payslip::getGrossPay));
        run.setTotalDeductions(sum(items, Payslip::getDeductionsTotal));
        run.setTotalNet(sum(items, Payslip::getNetPay));
    }

    private BigDecimal sum(List<Payslip> items, java.util.function.Function<Payslip, BigDecimal> field) {
        return items.stream().map(field).reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String employeeName(EmployeeProfile profile, User user) {
        if (user == null) return profile.getEmployeeCode();
        if (user.getFullName() == null || user.getFullName().isBlank()) return user.getUsername();
        return user.getFullName().trim();
    }

    private void requireDraft(PayrollRun run, String action) {
        if (run.getStatus() != PayrollRunStatus.DRAFT)
            throw new ConflictException("Only draft payroll runs can be " + action);
    }

    private EmployeeProfile requireProfile(Long userId) {
        return employees.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile", userId));
    }

    private PayrollRun requireRun(Long id) {
        return runs.findById(id).orElseThrow(() -> new ResourceNotFoundException("Payroll run", id));
    }

    private User requireUser(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeCurrency(String value) {
        return value == null || value.isBlank() ? "USD" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String likePattern(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
