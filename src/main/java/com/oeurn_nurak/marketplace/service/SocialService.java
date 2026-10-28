package com.oeurn_nurak.marketplace.service;

import com.oeurn_nurak.marketplace.dto.request.*;
import com.oeurn_nurak.marketplace.dto.response.PageDto;
import com.oeurn_nurak.marketplace.entity.*;
import com.oeurn_nurak.marketplace.entity.enums.*;
import com.oeurn_nurak.marketplace.exception.*;
import com.oeurn_nurak.marketplace.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SocialService {
    private final FriendshipRepository friendships;
    private final MessageRepository messages;
    private final CurrentUser current;
    private final AccountService accounts;
    private final ProductService products;
    private final NotificationService notifications;

    @Transactional(readOnly = true)
    public PageDto<Friendship> friends(Pageable page) {
        return PageDto.from(friendships.findByUserIdAndStatus(current.id(), FriendStatus.ACCEPTED, page));
    }

    @Transactional(readOnly = true)
    public PageDto<Friendship> requests(Pageable page) {
        return PageDto.from(friendships.findPendingRequestsForUser(current.id(), page));
    }

    public Friendship request(Long userId) {
        Long actor = current.id();
        if (actor.equals(userId)) throw new BadRequestException("You cannot befriend yourself");
        accounts.requireUser(userId);
        if (friendships.findBetweenUsers(actor, userId).isPresent())
            throw new ConflictException("A friendship or request already exists; delete it before sending another");
        Friendship friendship = friendships.save(Friendship.builder().requesterId(actor).addresseeId(userId)
                .status(FriendStatus.PENDING).build());
        notifications.send(userId, actor, NotifType.FRIEND_REQUEST, "New friend request", friendship.getId());
        return friendship;
    }

    public Friendship respond(Long id, FriendStatus status) {
        Friendship friendship = requireFriendship(id);
        current.requireOwner(friendship.getAddresseeId());
        if (friendship.getStatus() != FriendStatus.PENDING) throw new ConflictException("Request already answered");
        friendship.setStatus(status);
        return friendships.save(friendship);
    }

    private Friendship requireFriendship(Long id) {
        return friendships.findById(id).orElseThrow(() -> new ResourceNotFoundException("Friendship", id));
    }

    public void removeFriend(Long id) {
        Friendship friendship = requireFriendship(id);
        Long userId = current.id();
        if (!userId.equals(friendship.getRequesterId()) && !userId.equals(friendship.getAddresseeId()))
            throw new ForbiddenException("You are not part of this friendship");
        friendships.delete(friendship);
    }

    public Message send(Long receiver, SendMessageRequest request) {
        Long actor = current.id();
        if (actor.equals(receiver)) throw new BadRequestException("You cannot message yourself");
        User recipient = accounts.requireUser(receiver);
        if (!Boolean.TRUE.equals(recipient.getIsActive())) throw new BadRequestException("Recipient is inactive");
        if (request.getProductId() != null) products.require(request.getProductId());
        Message message = messages.save(Message.builder().senderId(actor).receiverId(receiver)
                .productId(request.getProductId()).content(request.getContent()).isRead(false).build());
        notifications.send(receiver, actor, NotifType.MESSAGE, "New message", message.getId(),
                null, request.getProductId(), 0);
        return message;
    }

    @Transactional(readOnly = true)
    public List<Message> inbox() { return messages.findInbox(current.id()); }

    @Transactional(readOnly = true)
    public PageDto<Message> conversation(Long userId, Pageable page) {
        accounts.requireUser(userId);
        return PageDto.from(messages.findConversationPage(current.id(), userId, page));
    }

    public void readConversation(Long userId) {
        accounts.requireUser(userId);
        List<Message> unread = messages.findBySenderIdAndReceiverIdAndIsReadFalse(userId, current.id());
        unread.forEach(message -> message.setIsRead(true));
        messages.saveAll(unread);
    }
}
