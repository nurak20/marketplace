package com.oeurn_nurak.marketplace.service;

import com.oeurn_nurak.marketplace.dto.response.NotificationResponse;
import com.oeurn_nurak.marketplace.dto.response.PageDto;
import com.oeurn_nurak.marketplace.entity.ListingPost;
import com.oeurn_nurak.marketplace.entity.Notification;
import com.oeurn_nurak.marketplace.entity.User;
import com.oeurn_nurak.marketplace.entity.enums.NotifType;
import com.oeurn_nurak.marketplace.exception.ForbiddenException;
import com.oeurn_nurak.marketplace.exception.ResourceNotFoundException;
import com.oeurn_nurak.marketplace.realtime.NotificationRealtimePublisher;
import com.oeurn_nurak.marketplace.repository.NotificationRepository;
import com.oeurn_nurak.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    /** Staff can see listing posts; regular users only see their own interactions. */
    private static final Set<String> STAFF_ROLES = Set.of(
            "ROLE_ADMIN", "ROLE_SUPER_ADMIN", "ROLE_HR", "ROLE_EMPLOYEE", "ROLE_MANAGER");

    private static final Set<NotifType> STAFF_TYPES =
            Collections.unmodifiableSet(EnumSet.allOf(NotifType.class));

    /**
     * Listing-post broadcasts are staff-only; every other type is addressed to
     * a specific recipient and must stay visible to that person.
     */
    private static final Set<NotifType> USER_TYPES = Collections.unmodifiableSet(
            EnumSet.complementOf(EnumSet.of(NotifType.POST)));

    private static final int MESSAGE_LIMIT = 280;

    private final NotificationRepository notifications;
    private final UserRepository users;
    private final CurrentUser current;
    private final NotificationRealtimePublisher realtime;

    public void send(Long recipient, Long actor, NotifType type, String message, Long reference) {
        send(recipient, actor, type, message, reference, null, null, 0);
    }

    public void send(Long recipient, Long actor, NotifType type, String message, Long reference,
                     Long postId, Long productId, int itemCount) {
        if (recipient.equals(actor)) return;
        Notification saved = notifications.saveAndFlush(Notification.builder()
                .userId(recipient)
                .actorId(actor)
                .type(type)
                .message(trim(message))
                .refId(reference)
                .postId(postId)
                .productId(productId)
                .itemCount(itemCount)
                .isRead(false)
                .build());
        publishCreated(saved);
    }

    /** A new listing is staff-only, so it fans out to every active staff member. */
    public void notifyPostCreated(ListingPost post, Long firstProductId, int itemCount, String content) {
        Long authorId = post.getUserId();
        String message = trim(content);
        if (message == null) message = "New listing";
        for (User staff : users.findActiveByRoleNames(STAFF_ROLES)) {
            if (staff.getId().equals(authorId)) continue;
            Notification saved = notifications.saveAndFlush(Notification.builder()
                    .userId(staff.getId())
                    .actorId(authorId)
                    .type(NotifType.POST)
                    .message(message)
                    .refId(post.getId())
                    .postId(post.getId())
                    .productId(firstProductId)
                    .itemCount(itemCount)
                    .isRead(false)
                    .build());
            publishCreated(saved);
        }
    }

    /** Password changes stay visible to the owner even though actor and recipient match. */
    public void notifyPasswordChanged() {
        Notification saved = notifications.saveAndFlush(Notification.builder()
                .userId(current.id())
                .actorId(current.id())
                .type(NotifType.PASSWORD_CHANGE)
                .message("Your password was changed")
                .itemCount(0)
                .isRead(false)
                .build());
        publishCreated(saved);
    }

    @Transactional(readOnly = true)
    public PageDto<NotificationResponse> list(Pageable page) {
        User user = current.get();
        Page<Notification> result = notifications.findByUserIdAndTypeInOrderByCreatedAtDesc(
                user.getId(), allowedTypes(user), page);
        Map<Long, User> actors = actors(result.getContent());
        return PageDto.from(result.map(item -> NotificationResponse.from(item, actors.get(item.getActorId()))));
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return unreadCount(current.get());
    }

    public NotificationResponse read(Long id) {
        Notification notification = notifications.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        current.requireOwner(notification.getUserId());
        User owner = current.get();
        if (!allowedTypes(owner).contains(notification.getType())) {
            throw new ForbiddenException("This notification is not available to your account");
        }

        boolean changed = !Boolean.TRUE.equals(notification.getIsRead());
        if (changed) {
            notification.setIsRead(true);
            notifications.saveAndFlush(notification);
        }
        NotificationResponse response = NotificationResponse.from(notification, actor(notification.getActorId()));
        if (changed) realtime.read(owner.getId(), notification.getId(), unreadCount(owner));
        return response;
    }

    public void readAll() {
        User user = current.get();
        int changed = notifications.markAllReadByUserId(user.getId());
        if (changed > 0) realtime.readAll(user.getId());
    }

    private void publishCreated(Notification notification) {
        User recipient = users.findById(notification.getUserId()).orElse(null);
        if (recipient == null) return;
        realtime.created(recipient.getId(),
                NotificationResponse.from(notification, actor(notification.getActorId())),
                unreadCount(recipient));
    }

    private User actor(Long actorId) {
        return actorId == null ? null : users.findById(actorId).orElse(null);
    }

    private Map<Long, User> actors(List<Notification> items) {
        Set<Long> ids = items.stream()
                .map(Notification::getActorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        Map<Long, User> result = new HashMap<>();
        users.findAllById(ids).forEach(user -> result.put(user.getId(), user));
        return result;
    }

    private long unreadCount(User user) {
        return notifications.countByUserIdAndTypeInAndIsReadFalse(user.getId(), allowedTypes(user));
    }

    private Set<NotifType> allowedTypes(User user) {
        boolean staff = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> STAFF_ROLES.contains(role.getName()));
        return staff ? STAFF_TYPES : USER_TYPES;
    }

    private static String trim(String value) {
        if (value == null) return null;
        String cleaned = value.trim();
        if (cleaned.isEmpty()) return null;
        return cleaned.length() <= MESSAGE_LIMIT ? cleaned : cleaned.substring(0, MESSAGE_LIMIT);
    }
}
