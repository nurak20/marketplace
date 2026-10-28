package com.oeurn_nurak.marketplace.service;

import com.oeurn_nurak.marketplace.dto.request.*;
import com.oeurn_nurak.marketplace.dto.response.MessageResponse;
import com.oeurn_nurak.marketplace.dto.response.PageDto;
import com.oeurn_nurak.marketplace.entity.*;
import com.oeurn_nurak.marketplace.entity.enums.*;
import com.oeurn_nurak.marketplace.exception.*;
import com.oeurn_nurak.marketplace.repository.*;
import com.oeurn_nurak.marketplace.realtime.NotificationRealtimePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class SocialService {
    private final FriendshipRepository friendships;
    private final MessageRepository messages;
    private final UserRepository users;
    private final CurrentUser current;
    private final AccountService accounts;
    private final ProductService products;
    private final NotificationService notifications;
    private final NotificationRealtimePublisher realtime;

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

    public MessageResponse send(Long receiver, SendMessageRequest request) {
        Long actor = current.id();
        if (actor.equals(receiver)) throw new BadRequestException("You cannot message yourself");
        User recipient = accounts.requireUser(receiver);
        if (!Boolean.TRUE.equals(recipient.getIsActive())) throw new BadRequestException("Recipient is inactive");
        if (request.getProductId() != null) products.require(request.getProductId());
        User sender = current.get();
        Message message = messages.saveAndFlush(Message.builder().senderId(actor).receiverId(receiver)
                .productId(request.getProductId()).content(request.getContent()).isRead(false).build());
        notifications.send(receiver, actor, NotifType.MESSAGE, message.getContent(), message.getId(),
                null, request.getProductId(), 0);
        realtime.messageCreated(message);
        return MessageResponse.from(message, sender, recipient);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> inbox() {
        return respond(messages.findInbox(current.id()));
    }

    @Transactional(readOnly = true)
    public PageDto<MessageResponse> conversation(Long userId, Pageable page) {
        accounts.requireUser(userId);
        Page<Message> result = messages.findConversationPage(current.id(), userId, page);
        Map<Long, User> participants = participants(result.getContent());
        return PageDto.from(result.map(message -> MessageResponse.from(message,
                participants.get(message.getSenderId()), participants.get(message.getReceiverId()))));
    }

    private List<MessageResponse> respond(List<Message> items) {
        Map<Long, User> participants = participants(items);
        return items.stream().map(message -> MessageResponse.from(message,
                participants.get(message.getSenderId()), participants.get(message.getReceiverId()))).toList();
    }

    private Map<Long, User> participants(List<Message> items) {
        Set<Long> ids = new HashSet<>();
        for (Message message : items) {
            ids.add(message.getSenderId());
            ids.add(message.getReceiverId());
        }
        Map<Long, User> result = new HashMap<>();
        users.findAllById(ids).forEach(user -> result.put(user.getId(), user));
        return result;
    }

    public void readConversation(Long userId) {
        accounts.requireUser(userId);
        List<Message> unread = messages.findBySenderIdAndReceiverIdAndIsReadFalse(userId, current.id());
        unread.forEach(message -> message.setIsRead(true));
        messages.saveAll(unread);
    }
}
