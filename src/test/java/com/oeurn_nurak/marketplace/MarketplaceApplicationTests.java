package com.oeurn_nurak.marketplace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oeurn_nurak.marketplace.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MarketplaceApplicationTests {
    private static final String API = "/api/v1";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired PermissionRepository permissions;

    record Account(long id, String token) {}

    JsonNode call(MockHttpServletRequestBuilder request, int status) throws Exception {
        String body = mvc.perform(request).andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("data");
    }

    MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder request, Object body) throws Exception {
        return request.contentType("application/json").content(json.writeValueAsBytes(body));
    }

    MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, Account account) {
        return request.header("Authorization", "Bearer " + account.token());
    }

    Account account(String username) throws Exception {
        JsonNode data = call(body(post(API + "/auth/register"), Map.of("username", username,
                "email", username + "@example.com", "password", "12345678", "fullName", username)), 201);
        long id = users.findByUsername(username).orElseThrow().getId();
        return new Account(id, data.path("token").asText());
    }

    Account admin() throws Exception {
        Account admin = account("administrator");
        var user = users.findById(admin.id()).orElseThrow();
        user.setRoles(new HashSet<>(Set.of(roles.findByName("ROLE_ADMIN").orElseThrow())));
        users.saveAndFlush(user);
        return admin;
    }

    Account staff(String username, String roleName) throws Exception {
        Account account = account(username);
        var user = users.findById(account.id()).orElseThrow();
        user.setRoles(new HashSet<>(Set.of(
                roles.findByName("ROLE_USER").orElseThrow(),
                roles.findByName(roleName).orElseThrow())));
        users.saveAndFlush(user);
        return account;
    }

    long product(Account seller) throws Exception {
        return call(body(auth(post(API + "/products"), seller), Map.of("title", "Phone", "price", 120.50,
                "category", "Electronics", "condition", "LIKE_NEW", "location", "Phnom Penh")), 201).path("id").asLong();
    }

    java.nio.file.Path storedImage(JsonNode image) {
        String url = image.path("imageUrl").asText();
        String name = url.substring(url.lastIndexOf("/uploads/") + "/uploads/".length());
        return java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "marketplace-test-uploads", name);
    }

    @Test void authenticationValidationAndPrivateProfiles() throws Exception {
        Account account = account("alice");
        JsonNode login = call(body(post(API + "/auth/login"), Map.of("username", "alice", "password", "12345678")), 200);
        assertThat(login.path("token").asText()).isNotBlank();
        call(body(post(API + "/auth/login"), Map.of("email", "alice@example.com", "password", "12345678")), 200);
        call(body(post(API + "/auth/login"), Map.of("username", "alice", "password", "wrong")), 401);
        call(body(post(API + "/auth/login"), Map.of("password", "12345678")), 400);
        call(body(post(API + "/auth/register"), Map.of("username", "alice", "email", "alice@example.com", "password", "12345678")), 409);
        call(body(post(API + "/auth/register"), Map.of("username", "a", "email", "bad", "password", "short")), 400);
        JsonNode me = call(auth(get(API + "/users/me"), account), 200);
        assertThat(me.path("email").asText()).isEqualTo("alice@example.com");
        assertThat(me.has("password")).isFalse();
        JsonNode profile = call(auth(get(API + "/users/" + account.id()), account), 200);
        assertThat(profile.has("email")).isFalse();
        assertThat(profile.has("password")).isFalse();
        call(body(auth(patch(API + "/users/me"), account), Map.of("bio", "Seller", "phone", "012345678")), 200);
        JsonNode updated = call(body(auth(patch(API + "/users/me"), account),
                Map.of("firstName", "Alice", "lastName", "Nguyen")), 200);
        assertThat(updated.path("account").path("profile").path("firstName").asText()).isEqualTo("Alice");
        assertThat(updated.path("account").path("profile").path("lastName").asText()).isEqualTo("Nguyen");
        assertThat(updated.path("account").path("profile").path("fullName").asText()).isEqualTo("Alice Nguyen");
        assertThat(updated.has("token")).isFalse();
        assertThat(call(auth(get(API + "/users/me/permissions"), account), 200).toString()).contains("product:write");
        call(get(API + "/users/me"), 401);
        call(get(API + "/users/me").header("Authorization", "Bearer invalid"), 401);
        call(auth(get(API + "/admin/users"), account), 403);
        call(auth(get(API + "/does-not-exist"), account), 404);
        call(auth(post(API + "/products"), account).contentType("application/json").content("{"), 400);
        call(get(API + "/products/no-number"), 400);
        call(get(API + "/products?sort=unknownProperty"), 400);

        Account other = account("bobby");
        call(body(auth(patch(API + "/users/me"), account), Map.of("email", "bobby@example.com")), 409);
        JsonNode renamed = call(body(auth(patch(API + "/users/me"), account),
                Map.of("email", "alice.new@example.com")), 200);
        String refreshed = renamed.path("token").asText();
        assertThat(refreshed).isNotBlank();
        assertThat(other.id()).isNotEqualTo(account.id());
        assertThat(call(get(API + "/users/me").header("Authorization", "Bearer " + refreshed), 200)
                .path("email").asText()).isEqualTo("alice.new@example.com");
    }

    @Test void productLifecycleFiltersAndOwnership() throws Exception {
        Account seller = account("seller");
        Account other = account("other");
        long id = product(seller);
        JsonNode detail = call(get(API + "/products/" + id), 200).path("product");
        assertThat(detail.path("condition").asText()).isEqualTo("LIKE_NEW");
        assertThat(detail.path("status").asText()).isEqualTo("AVAILABLE");
        assertThat(detail.has("searchVector")).isFalse();
        assertThat(call(get(API + "/products?keyword=phone&minPrice=100&maxPrice=150&condition=LIKE_NEW&category=Electronics"), 200)
                .path("totalElements").asLong()).isEqualTo(1);
        assertThat(call(get(API + "/products?keyword=missing"), 200).path("totalElements").asLong()).isZero();
        call(get(API + "/products?minPrice=200&maxPrice=10"), 400);
        call(body(auth(patch(API + "/products/" + id), other), Map.of("title", "Stolen")), 403);
        call(body(auth(patch(API + "/products/" + id), seller), Map.of("title", "Updated phone", "price", 100)), 200);
        call(body(auth(patch(API + "/products/" + id), seller), Map.of("title", " ")), 400);
        call(body(auth(patch(API + "/products/" + id + "/status"), seller), Map.of("status", "SOLD")), 200);
        call(body(auth(patch(API + "/products/" + id + "/status"), seller), Map.of("status", "DELETED")), 400);
        call(auth(delete(API + "/products/" + id), other), 403);
        call(auth(delete(API + "/products/" + id), seller), 200);
        call(get(API + "/products/" + id), 404);
        assertThat(call(get(API + "/products"), 200).path("totalElements").asLong()).isZero();
    }

    @Test void productImagesValidateContentAndOwnership() throws Exception {
        Account seller = account("seller");
        Account other = account("other");
        long id = product(seller);
        var bitmap = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var bytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(bitmap, "png", bytes);
        var file = new MockMultipartFile("file", "image.png", "image/png", bytes.toByteArray());
        JsonNode image = call(auth(multipart(API + "/products/" + id + "/images").file(file), seller), 201);
        long imageId = image.path("id").asLong();
        assertThat(image.path("isPrimary").asBoolean()).isTrue();
        mvc.perform(get(image.path("imageUrl").asText())).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        call(get(API + "/products/" + id + "/images"), 200);
        call(auth(patch(API + "/products/" + id + "/images/" + imageId + "/primary"), seller), 200);
        call(auth(multipart(API + "/products/" + id + "/images").file(file), other), 403);
        var invalid = new MockMultipartFile("file", "bad.png", "image/png", "not an image".getBytes());
        call(auth(multipart(API + "/products/" + id + "/images").file(invalid), seller), 400);
        call(auth(delete(API + "/products/" + id + "/images/" + imageId), other), 403);
        call(auth(delete(API + "/products/" + id + "/images/" + imageId), seller), 200);
        assertThat(call(get(API + "/products/" + id + "/images"), 200)).isEmpty();
        java.nio.file.Files.deleteIfExists(storedImage(image));
    }

    @Test void productImagesApplyExifOrientation() throws Exception {
        Account seller = account("seller");
        long id = product(seller);
        var jpeg = TestImages.jpeg(TestImages.pixels(4, 2, 1, 2, 3, 4, 5, 6, 7, 8));
        var file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", TestImages.withExif(jpeg, 6, true));
        JsonNode image = call(auth(multipart(API + "/products/" + id + "/images").file(file), seller), 201);
        var stored = storedImage(image);
        assertThat(stored).exists();
        var saved = javax.imageio.ImageIO.read(stored.toFile());
        assertThat(saved.getWidth()).isEqualTo(2);
        assertThat(saved.getHeight()).isEqualTo(4);
        java.nio.file.Files.deleteIfExists(stored);
    }

    @Test void postsCommentsReactionsAndSoftDeletion() throws Exception {
        Account owner = account("owner");
        Account visitor = account("visitor");
        long productId = product(owner);
        long postId = call(body(auth(post(API + "/posts"), owner), Map.of("content", "For sale", "productIds", List.of(productId, productId))), 201)
                .path("post").path("id").asLong();
        assertThat(call(get(API + "/posts/" + postId), 200).path("products").size()).isEqualTo(1);
        call(get(API + "/posts"), 200);
        call(get(API + "/posts?userId=" + owner.id()), 200);
        call(body(auth(post(API + "/posts"), visitor), Map.of("productIds", List.of(productId))), 403);
        call(body(auth(put(API + "/posts/" + postId), owner), Map.of("content", "Updated", "productIds", List.of(productId))), 200);
        call(body(auth(put(API + "/posts/" + postId + "/reaction"), visitor), Map.of("reactionType", "LOVE")), 200);
        call(body(auth(put(API + "/posts/" + postId + "/reaction"), visitor), Map.of("reactionType", "WOW")), 200);
        assertThat(call(get(API + "/posts/" + postId), 200).path("reactionCount").asLong()).isEqualTo(1);
        long commentId = call(body(auth(post(API + "/posts/" + postId + "/comments"), visitor), Map.of("content", "Interested")), 201).path("id").asLong();
        call(get(API + "/posts/" + postId + "/comments"), 200);
        call(body(auth(patch(API + "/comments/" + commentId), visitor), Map.of("content", "Updated comment")), 200);
        call(auth(delete(API + "/comments/" + commentId), owner), 403);
        call(auth(delete(API + "/comments/" + commentId), visitor), 200);
        assertThat(call(get(API + "/posts/" + postId + "/comments"), 200).path("totalElements").asLong()).isZero();
        call(auth(delete(API + "/posts/" + postId + "/reaction"), visitor), 200);
        call(auth(delete(API + "/posts/" + postId), visitor), 403);
        call(auth(delete(API + "/posts/" + postId), owner), 200);
        JsonNode removedPost = call(get(API + "/posts/" + postId), 200);
        assertThat(removedPost.path("post").path("removed").asBoolean()).isTrue();
        assertThat(removedPost.path("post").path("content").asText())
                .isEqualTo("This listing is no longer available.");
        assertThat(removedPost.path("products")).isEmpty();
        call(get(API + "/posts/" + postId + "/comments"), 404);
    }

    @Test void wishlistReviewsAndNotifications() throws Exception {
        Account seller = account("seller");
        Account buyer = account("buyer");
        long id = product(seller);
        call(auth(put(API + "/wishlist/" + id), buyer), 200);
        call(auth(put(API + "/wishlist/" + id), buyer), 200);
        assertThat(call(auth(get(API + "/wishlist"), buyer), 200).path("totalElements").asLong()).isEqualTo(1);
        call(body(auth(post(API + "/products/" + id + "/reviews"), seller), Map.of("rating", 5)), 400);
        call(body(auth(post(API + "/products/" + id + "/reviews"), buyer), Map.of("rating", 6)), 400);
        long reviewId = call(body(auth(post(API + "/products/" + id + "/reviews"), buyer), Map.of("rating", 5, "comment", "Great")), 201).path("id").asLong();
        call(body(auth(post(API + "/products/" + id + "/reviews"), buyer), Map.of("rating", 4)), 409);
        assertThat(call(get(API + "/products/" + id + "/reviews"), 200).path("totalElements").asLong()).isEqualTo(1);
        call(auth(get(API + "/users/" + seller.id() + "/reviews"), buyer), 200);
        JsonNode notifications = call(auth(get(API + "/notifications"), seller), 200);
        long notificationId = notifications.path("items").get(0).path("id").asLong();
        call(auth(patch(API + "/notifications/" + notificationId + "/read"), buyer), 403);
        call(auth(patch(API + "/notifications/" + notificationId + "/read"), seller), 200);
        call(auth(patch(API + "/notifications/read-all"), seller), 200);
        call(auth(delete(API + "/reviews/" + reviewId), seller), 403);
        call(auth(delete(API + "/reviews/" + reviewId), buyer), 200);
        call(auth(delete(API + "/wishlist/" + id), buyer), 200);
        assertThat(call(auth(get(API + "/wishlist"), buyer), 200).path("totalElements").asLong()).isZero();
    }

    @Test void friendshipsAndPrivateConversations() throws Exception {
        Account alice = account("alice");
        Account bob = account("bobby");
        Account outsider = account("outsider");
        call(auth(post(API + "/friends/requests/" + alice.id()), alice), 400);
        long friendshipId = call(auth(post(API + "/friends/requests/" + bob.id()), alice), 201).path("id").asLong();
        call(auth(post(API + "/friends/requests/" + alice.id()), bob), 409);
        call(auth(get(API + "/friends/requests"), bob), 200);
        call(auth(patch(API + "/friends/requests/" + friendshipId + "/accept"), alice), 403);
        call(auth(patch(API + "/friends/requests/" + friendshipId + "/accept"), bob), 200);
        call(auth(patch(API + "/friends/requests/" + friendshipId + "/decline"), bob), 409);
        assertThat(call(auth(get(API + "/friends"), alice), 200).path("totalElements").asLong()).isEqualTo(1);
        call(body(auth(post(API + "/messages/" + bob.id()), alice), Map.of("content", "Hello")), 201);
        call(body(auth(post(API + "/messages/" + alice.id()), alice), Map.of("content", "Hello")), 400);
        assertThat(call(auth(get(API + "/messages"), bob), 200).size()).isEqualTo(1);
        assertThat(call(auth(get(API + "/messages/" + alice.id()), bob), 200).path("totalElements").asLong()).isEqualTo(1);
        assertThat(call(auth(get(API + "/messages/" + alice.id()), outsider), 200).path("totalElements").asLong()).isZero();
        call(auth(patch(API + "/messages/" + alice.id() + "/read"), bob), 200);
        assertThat(call(auth(get(API + "/messages/" + alice.id()), bob), 200).path("items").get(0).path("isRead").asBoolean()).isTrue();
        call(auth(delete(API + "/friends/" + friendshipId), outsider), 403);
        call(auth(delete(API + "/friends/" + friendshipId), alice), 200);
        long second = call(auth(post(API + "/friends/requests/" + alice.id()), bob), 201).path("id").asLong();
        call(auth(patch(API + "/friends/requests/" + second + "/decline"), alice), 200);
    }

    @Test void rolePermissionCrudAssignmentsAndLiveEnforcement() throws Exception {
        Account admin = admin();
        Account member = account("member");
        long roleId = call(body(auth(post(API + "/admin/roles"), admin), Map.of("name", "ROLE_SELLER", "description", "Seller role")), 201).path("id").asLong();
        long permissionId = call(body(auth(post(API + "/admin/permissions"), admin), Map.of("name", "analytics:read", "description", "Read analytics")), 201).path("id").asLong();
        call(auth(get(API + "/admin/roles"), admin), 200);
        call(auth(get(API + "/admin/roles/" + roleId), admin), 200);
        call(auth(get(API + "/admin/permissions"), admin), 200);
        call(auth(get(API + "/admin/permissions/" + permissionId), admin), 200);
        call(body(auth(put(API + "/admin/permissions/" + permissionId), admin), Map.of("name", "analytics:read", "description", "Updated")), 200);
        call(body(auth(put(API + "/admin/roles/" + roleId), admin), Map.of("name", "ROLE_SELLER", "description", "Updated")), 200);
        call(auth(put(API + "/admin/roles/" + roleId + "/permissions/" + permissionId), admin), 200);
        call(auth(delete(API + "/admin/permissions/" + permissionId), admin), 409);
        call(auth(get(API + "/admin/roles/" + roleId + "/permissions"), admin), 200);
        long productWrite = permissions.findByName("product:write").orElseThrow().getId();
        call(body(auth(put(API + "/admin/roles/" + roleId + "/permissions"), admin), Map.of("ids", List.of(productWrite))), 200);
        call(body(auth(put(API + "/admin/users/" + member.id() + "/roles"), admin), Map.of("ids", List.of(roleId))), 200);
        product(member); // The existing JWT receives the new permissions on the next request.
        call(auth(delete(API + "/admin/roles/" + roleId + "/permissions/" + productWrite), admin), 200);
        call(body(auth(post(API + "/products"), member), Map.of("title", "Denied", "price", 1)), 403);
        call(auth(get(API + "/admin/users/" + member.id() + "/roles"), admin), 200);
        call(auth(get(API + "/admin/users/" + member.id() + "/permissions"), admin), 200);
        long userRoleId = roles.findByName("ROLE_USER").orElseThrow().getId();
        call(auth(put(API + "/admin/users/" + member.id() + "/roles/" + userRoleId), admin), 200);
        call(auth(delete(API + "/admin/roles/" + roleId), admin), 409);
        call(auth(delete(API + "/admin/users/" + member.id() + "/roles/" + roleId), admin), 200);
        call(auth(delete(API + "/admin/roles/" + roleId), admin), 200);
        call(auth(delete(API + "/admin/permissions/" + permissionId), admin), 200);
        call(auth(delete(API + "/admin/roles/" + userRoleId), admin), 400);
        long adminRoleId = roles.findByName("ROLE_ADMIN").orElseThrow().getId();
        call(auth(delete(API + "/admin/users/" + admin.id() + "/roles/" + adminRoleId), admin), 400);
        call(body(auth(put(API + "/admin/roles/" + adminRoleId + "/permissions"), admin), Map.of("ids", List.of())), 400);
        call(auth(get(API + "/admin/roles"), member), 403);
    }

    @Test void adminUserManagementAndDisabledTokens() throws Exception {
        Account admin = admin();
        Account member = account("member");
        call(auth(get(API + "/admin/users"), admin), 200);
        call(auth(get(API + "/admin/users/" + member.id()), admin), 200);
        call(body(auth(patch(API + "/admin/users/" + admin.id() + "/active"), admin), Map.of("active", false)), 400);
        call(body(auth(patch(API + "/admin/users/" + member.id() + "/active"), admin), Map.of("active", false)), 200);
        call(auth(get(API + "/users/me"), member), 401);
        call(body(post(API + "/auth/login"), Map.of("username", "member", "password", "12345678")), 401);
        call(body(auth(patch(API + "/admin/users/" + member.id() + "/active"), admin), Map.of("active", true)), 200);
        call(auth(get(API + "/users/me"), member), 200);
    }

    @Test void payrollEnrollmentRunsApprovalAndSelfAccess() throws Exception {
        Account hr = staff("hrpay", "ROLE_HR");
        Account manager = staff("mgrpay", "ROLE_MANAGER");
        Account employee = staff("emppay", "ROLE_EMPLOYEE");
        Account outsider = account("payoutsider");

        call(body(auth(post(API + "/payroll/employees"), employee), Map.of(
                "userId", employee.id(), "baseSalary", 1000)), 403);

        JsonNode profile = call(body(auth(post(API + "/payroll/employees"), hr), Map.of(
                "userId", employee.id(),
                "jobTitle", "Clerk",
                "department", "Operations",
                "baseSalary", 1000,
                "allowance", 100,
                "hireDate", "2024-01-15")), 201);
        assertThat(profile.path("employeeCode").asText()).isEqualTo("EMP-%04d".formatted(employee.id()));
        call(body(auth(post(API + "/payroll/employees"), hr), Map.of(
                "userId", employee.id(), "baseSalary", 1000)), 409);
        call(auth(get(API + "/payroll/employees"), employee), 403);
        assertThat(call(auth(get(API + "/payroll/employees?q=emppay"), hr), 200)
                .path("totalElements").asLong()).isEqualTo(1);
        call(auth(get(API + "/payroll/employees/" + employee.id()), manager), 200);

        int futureYear = java.time.LocalDate.now().getYear() + 1;
        call(body(auth(post(API + "/payroll/runs"), hr), Map.of("year", futureYear, "month", 1)), 400);

        JsonNode run = call(body(auth(post(API + "/payroll/runs"), hr), Map.of(
                "year", 2026, "month", 3, "notes", "March payroll")), 201);
        long runId = run.path("id").asLong();
        assertThat(run.path("totalGross").decimalValue()).isEqualByComparingTo("1100.00");
        assertThat(run.path("totalNet").decimalValue()).isEqualByComparingTo("1100.00");
        call(body(auth(post(API + "/payroll/runs"), hr), Map.of("year", 2026, "month", 3)), 409);

        JsonNode slips = call(auth(get(API + "/payroll/runs/" + runId + "/payslips"), hr), 200);
        assertThat(slips.path("totalElements").asLong()).isEqualTo(1);
        long payslipId = slips.path("items").get(0).path("id").asLong();

        JsonNode adjusted = call(body(auth(patch(API + "/payroll/payslips/" + payslipId), hr), Map.of(
                "bonus", 100, "tax", 50, "otherDeductions", 20)), 200);
        assertThat(adjusted.path("grossPay").decimalValue()).isEqualByComparingTo("1200.00");
        assertThat(adjusted.path("deductionsTotal").decimalValue()).isEqualByComparingTo("70.00");
        assertThat(adjusted.path("netPay").decimalValue()).isEqualByComparingTo("1130.00");
        call(body(auth(patch(API + "/payroll/payslips/" + payslipId), hr), Map.of(
                "otherDeductions", 5000)), 400);

        // Draft payslips are internal; employees cannot see them yet.
        assertThat(call(auth(get(API + "/payroll/me/payslips"), employee), 200)
                .path("totalElements").asLong()).isZero();
        call(auth(get(API + "/payroll/me/payslips/" + payslipId), employee), 404);
        call(auth(get(API + "/payroll/me/payslips/" + payslipId), outsider), 404);
        call(auth(get(API + "/payroll/payslips/" + payslipId), employee), 403);

        call(auth(get(API + "/payroll/runs/" + runId), manager), 200);
        call(body(auth(patch(API + "/payroll/payslips/" + payslipId), manager), Map.of("bonus", 1)), 403);
        call(auth(post(API + "/payroll/runs/" + runId + "/approve"), hr), 403);
        JsonNode approved = call(auth(post(API + "/payroll/runs/" + runId + "/approve"), manager), 200);
        assertThat(approved.path("status").asText()).isEqualTo("APPROVED");
        assertThat(call(auth(get(API + "/payroll/me/payslips"), employee), 200)
                .path("totalElements").asLong()).isEqualTo(1);
        call(auth(get(API + "/payroll/me/payslips/" + payslipId), employee), 200);
        call(body(auth(patch(API + "/payroll/payslips/" + payslipId), hr), Map.of("bonus", 1)), 409);
        call(auth(post(API + "/payroll/runs/" + runId + "/approve"), manager), 409);

        JsonNode paid = call(auth(post(API + "/payroll/runs/" + runId + "/pay"), manager), 200);
        assertThat(paid.path("status").asText()).isEqualTo("PAID");
        assertThat(call(auth(get(API + "/payroll/me/payslips"), employee), 200)
                .path("items").get(0).path("runStatus").asText()).isEqualTo("PAID");
        assertThat(call(auth(get(API + "/notifications"), employee), 200)
                .path("items").get(0).path("type").asText()).isEqualTo("PAYROLL");

        JsonNode spare = call(body(auth(post(API + "/payroll/runs"), hr), Map.of(
                "year", 2026, "month", 5)), 201);
        long spareId = spare.path("id").asLong();
        call(auth(post(API + "/payroll/runs/" + spareId + "/regenerate"), hr), 200);
        call(auth(post(API + "/payroll/runs/" + spareId + "/cancel"), hr), 200);
        call(auth(post(API + "/payroll/runs/" + spareId + "/approve"), manager), 409);

        call(body(auth(patch(API + "/payroll/employees/" + employee.id() + "/status"), hr), Map.of(
                "status", "TERMINATED")), 200);
        call(body(auth(post(API + "/payroll/runs"), hr), Map.of("year", 2026, "month", 4)), 400);
    }

    @Test void reportsRequirePermissionAndReturnConsistentAggregates() throws Exception {
        Account admin = admin();
        Account hr = staff("hrreports", "ROLE_HR");
        Account employee = staff("empreports", "ROLE_EMPLOYEE");
        Account member = account("reportsmember");

        call(auth(get(API + "/reports/overview"), member), 403);
        call(auth(get(API + "/reports/overview"), employee), 403);

        JsonNode overview = call(auth(get(API + "/reports/overview"), hr), 200);
        assertThat(overview.path("totalUsers").asLong()).isPositive();
        assertThat(overview.path("activeUsers").asLong()).isLessThanOrEqualTo(overview.path("totalUsers").asLong());
        assertThat(overview.path("availableProducts").asLong() + overview.path("soldProducts").asLong())
                .isLessThanOrEqualTo(overview.path("products").asLong());
        assertThat(overview.path("paidGross").decimalValue()).isGreaterThanOrEqualTo(java.math.BigDecimal.ZERO);
        assertThat(call(auth(get(API + "/reports/overview"), admin), 200)
                .path("totalUsers").asLong()).isEqualTo(overview.path("totalUsers").asLong());

        assertThat(call(auth(get(API + "/reports/product-status"), admin), 200)).isNotNull();
        assertThat(call(auth(get(API + "/reports/categories"), admin), 200)).isNotNull();
        assertThat(call(auth(get(API + "/reports/top-sellers?limit=5"), admin), 200)).isNotNull();
        assertThat(call(auth(get(API + "/reports/payroll/departments"), admin), 200)).isNotNull();
        assertThat(call(auth(get(API + "/reports/payroll/runs?limit=6"), admin), 200)).isNotNull();
        assertThat(call(auth(get(API + "/reports/payroll/headcount"), admin), 200)).isNotNull();

        JsonNode activity = call(auth(get(API + "/reports/monthly-activity?months=3"), admin), 200);
        assertThat(activity.size()).isEqualTo(3);
        JsonNode engagement = call(auth(get(API + "/reports/engagement?months=3"), admin), 200);
        assertThat(engagement.size()).isEqualTo(3);
        assertThat(activity.get(0).path("month").asText()).matches("\\d{4}-\\d{2}");

        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Nurak MK27 Marketplace API"))
                .andExpect(jsonPath("$.paths['/api/v1/reports/overview']").exists());
        call(get(API + "/reports/overview"), 401);
    }
}
