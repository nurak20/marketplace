package com.oeurn_nurak.marketplace.service;

import com.oeurn_nurak.marketplace.dto.response.PageDto;
import com.oeurn_nurak.marketplace.entity.Notification;
import com.oeurn_nurak.marketplace.entity.enums.NotifType;
import com.oeurn_nurak.marketplace.exception.ResourceNotFoundException;
import com.oeurn_nurak.marketplace.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {
    private final NotificationRepository notifications;
    private final CurrentUser current;

    public void send(Long recipient, Long actor, NotifType type, String message, Long reference) {
        if (!recipient.equals(actor)) notifications.save(Notification.builder().userId(recipient)
                .type(type).message(message).refId(reference).isRead(false).build());
    }

    @Transactional(readOnly = true)
    public PageDto<Notification> list(Pageable page) {
        return PageDto.from(notifications.findByUserIdOrderByCreatedAtDesc(current.id(), page));
    }

    public Notification read(Long id) {
        Notification notification = notifications.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        current.requireOwner(notification.getUserId());
        notification.setIsRead(true);
        return notifications.save(notification);
    }

    public void readAll() { notifications.markAllReadByUserId(current.id()); }
}
