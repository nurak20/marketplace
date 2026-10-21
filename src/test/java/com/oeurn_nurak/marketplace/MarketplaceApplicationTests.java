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

    long product(Account seller) throws Exception {
        return call(body(auth(post(API + "/products"), seller), Map.of("title", "Phone", "price", 120.50,
                "category", "Electronics", "condition", "LIKE_NEW", "location", "Phnom Penh")), 201).path("id").asLong();
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
        assertThat(call(auth(get(API + "/users/me/permissions"), account), 200).toString()).contains("product:write");
        call(get(API + "/users/me"), 401);
        call(get(API + "/users/me").header("Authorization", "Bearer invalid"), 401);
        call(auth(get(API + "/admin/users"), account), 403);
        call(auth(get(API + "/does-not-exist"), account), 404);
        call(auth(post(API + "/products"), account).contentType("application/json").content("{"), 400);
        call(get(API + "/products/no-number"), 400);
        call(get(API + "/products?sort=unknownProperty"), 400);
    }

    @Test void productLifecycleFiltersAndOwnership() throws Exception {
        Account seller = account("seller");
        Account other = account("other");
        long id = product(seller);
        JsonNode detail = call(get(API + "/products/" + id), 200);
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
        java.nio.file.Files.deleteIfExists(java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "marketplace-test-uploads", image.path("imageUrl").asText().substring("/uploads/".length())));
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
        call(get(API + "/posts/" + postId), 404);
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
}
