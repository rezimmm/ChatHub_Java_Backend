package com.chathub.service;

import com.chathub.dto.MessageCreateRequest;
import com.chathub.dto.MessageUpdateRequest;
import com.chathub.model.Channel;
import com.chathub.model.Message;
import com.chathub.model.Reaction;
import com.chathub.model.User;
import com.chathub.repository.ChannelRepository;
import com.chathub.repository.MessageRepository;
import com.chathub.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChannelRepository channelRepository;
    private final ChatWebSocketHandler wsHandler;

    public List<Map<String, Object>> getMessages(String channelId, int limit, String before, User currentUser) {
        Channel ch = findChannelOrThrow(channelId);
        if (!ch.getMembers().contains(currentUser.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");

        Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
        List<Message> messages = before != null
                ? messageRepository.findByChannelIdBefore(channelId, before, sort)
                : messageRepository.findByChannelId(channelId, sort);

        List<Message> limited = messages.stream().limit(limit).collect(Collectors.toList());
        Collections.reverse(limited);
        return limited.stream().map(this::toMap).collect(Collectors.toList());
    }

    public Map<String, Object> createMessage(MessageCreateRequest req, User currentUser) {
        Channel ch = findChannelOrThrow(req.getChannelId());
        if (!ch.getMembers().contains(currentUser.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");

        String content = sanitize(req.getContent());
        if (content == null || content.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty");

        Message msg = Message.builder()
                .channelId(req.getChannelId())
                .userId(currentUser.getId())
                .username(currentUser.getUsername())
                .content(content)
                .avatarColor(currentUser.getAvatarColor())
                .avatarUrl(currentUser.getAvatarUrl())
                .replyTo(req.getReplyTo())
                .threadId(req.getThreadId())
                .readBy(new ArrayList<>(List.of(currentUser.getId())))
                .build();

        messageRepository.save(msg);

        // Thread reply: increment parent reply_count
        if (req.getThreadId() != null) {
            messageRepository.findByMessageId(req.getThreadId()).ifPresent(parent -> {
                parent.setReplyCount(parent.getReplyCount() + 1);
                messageRepository.save(parent);
                Map<String, Object> threadEvent = new HashMap<>();
                threadEvent.put("type", "thread_updated");
                threadEvent.put("data", toMap(parent));
                wsHandler.broadcastToChannel(threadEvent, req.getChannelId());
            });
        }

        // Broadcast new message
        Map<String, Object> msgEvent = new HashMap<>();
        msgEvent.put("type", "message");
        msgEvent.put("data", toMap(msg));
        wsHandler.broadcastToChannel(msgEvent, req.getChannelId());

        // Update unread counts for other members
        ch.getMembers().stream()
                .filter(memberId -> !memberId.equals(currentUser.getId()))
                .forEach(memberId -> {
                    ch.getUnreadCount().merge(memberId, 1, Integer::sum);
                });
        channelRepository.save(ch);

        return toMap(msg);
    }

    public Map<String, Object> updateMessage(String messageId, MessageUpdateRequest req, User currentUser) {
        Message msg = findMessageOrThrow(messageId);
        if (!msg.getUserId().equals(currentUser.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized");

        String content = sanitize(req.getContent());
        if (content == null || content.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty");

        msg.setContent(content);
        msg.setEdited(true);
        msg.setEditedAt(Instant.now().toString());
        messageRepository.save(msg);

        Map<String, Object> event = new HashMap<>();
        event.put("type", "message_updated");
        event.put("data", toMap(msg));
        wsHandler.broadcastToChannel(event, msg.getChannelId());

        return toMap(msg);
    }

    public Map<String, Object> deleteMessage(String messageId, User currentUser) {
        Message msg = findMessageOrThrow(messageId);
        if (!msg.getUserId().equals(currentUser.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized");

        messageRepository.delete(msg);

        Map<String, Object> event = new HashMap<>();
        event.put("type", "message_deleted");
        event.put("message_id", messageId);
        wsHandler.broadcastToChannel(event, msg.getChannelId());

        return Map.of("success", true);
    }

    public Map<String, Object> toggleReaction(String messageId, String emoji, User currentUser) {
        Message msg = findMessageOrThrow(messageId);

        Reaction existing = msg.getReactions().stream()
                .filter(r -> r.getUserId().equals(currentUser.getId()) && r.getEmoji().equals(emoji))
                .findFirst().orElse(null);

        if (existing != null) {
            msg.getReactions().remove(existing);
        } else {
            msg.getReactions().add(Reaction.builder()
                    .emoji(emoji)
                    .userId(currentUser.getId())
                    .username(currentUser.getUsername())
                    .build());
        }

        messageRepository.save(msg);

        Map<String, Object> event = new HashMap<>();
        event.put("type", "reaction_updated");
        event.put("data", toMap(msg));
        wsHandler.broadcastToChannel(event, msg.getChannelId());

        return toMap(msg);
    }

    public Map<String, Object> togglePin(String messageId, User currentUser) {
        Message msg = findMessageOrThrow(messageId);
        msg.setPinned(!msg.isPinned());
        messageRepository.save(msg);

        Map<String, Object> event = new HashMap<>();
        event.put("type", "message_updated");
        event.put("data", toMap(msg));
        wsHandler.broadcastToChannel(event, msg.getChannelId());

        return Map.of("pinned", msg.isPinned());
    }

    public Map<String, Object> markMessageRead(String messageId, User currentUser) {
        Message msg = findMessageOrThrow(messageId);
        if (!msg.getReadBy().contains(currentUser.getId())) {
            msg.getReadBy().add(currentUser.getId());
            messageRepository.save(msg);

            Map<String, Object> event = new HashMap<>();
            event.put("type", "message_read");
            event.put("message_id", messageId);
            event.put("user_id", currentUser.getId());
            event.put("username", currentUser.getUsername());
            wsHandler.broadcastToChannel(event, msg.getChannelId());
        }
        return Map.of("success", true);
    }

    public Map<String, Object> markAllRead(String channelId, User currentUser) {
        Channel ch = findChannelOrThrow(channelId);
        if (!ch.getMembers().contains(currentUser.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");

        messageRepository.findByChannelId(channelId, Sort.unsorted()).forEach(msg -> {
            if (!msg.getReadBy().contains(currentUser.getId())) {
                msg.getReadBy().add(currentUser.getId());
                messageRepository.save(msg);
            }
        });

        ch.getUnreadCount().put(currentUser.getId(), 0);
        channelRepository.save(ch);

        return Map.of("success", true);
    }

    public List<Map<String, Object>> getThread(String messageId, User currentUser) {
        Message parent = findMessageOrThrow(messageId);
        Channel ch = findChannelOrThrow(parent.getChannelId());
        if (!ch.getMembers().contains(currentUser.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");

        return messageRepository.findByThreadId(messageId, Sort.by(Sort.Direction.ASC, "timestamp"))
                .stream().map(this::toMap).collect(Collectors.toList());
    }

    public List<Map<String, Object>> searchMessages(String query, User currentUser) {
        List<String> channelIds = channelRepository.findByMembersContaining(currentUser.getId())
                .stream().map(Channel::getId).collect(Collectors.toList());

        return messageRepository.searchInChannels(channelIds, query, Sort.by(Sort.Direction.DESC, "timestamp"))
                .stream().limit(50).map(this::toMap).collect(Collectors.toList());
    }

    public void deleteMessagesByChannel(String channelId) {
        messageRepository.deleteByChannelId(channelId);
        
        // Broadcast clear event
        Map<String, Object> event = new HashMap<>();
        event.put("type", "messages_cleared");
        event.put("channel_id", channelId);
        wsHandler.broadcastToChannel(event, channelId);
    }

    private Message findMessageOrThrow(String id) {
        return messageRepository.findByMessageId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
    }

    private Channel findChannelOrThrow(String id) {
        return channelRepository.findByChannelId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Channel not found"));
    }

    public Map<String, Object> toMap(Message m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", m.getId());
        map.put("channel_id", m.getChannelId());
        map.put("user_id", m.getUserId());
        map.put("username", m.getUsername());
        map.put("content", m.getContent());
        map.put("timestamp", m.getTimestamp());
        map.put("avatar_color", m.getAvatarColor());
        map.put("avatar_url", m.getAvatarUrl());
        map.put("edited", m.isEdited());
        map.put("edited_at", m.getEditedAt());
        map.put("reactions", m.getReactions().stream().map(r -> {
            Map<String, Object> rm = new LinkedHashMap<>();
            rm.put("emoji", r.getEmoji());
            rm.put("user_id", r.getUserId());
            rm.put("username", r.getUsername());
            return rm;
        }).collect(Collectors.toList()));
        map.put("file_url", m.getFileUrl());
        map.put("file_name", m.getFileName());
        map.put("file_type", m.getFileType());
        map.put("pinned", m.isPinned());
        map.put("reply_to", m.getReplyTo());
        map.put("read_by", m.getReadBy());
        map.put("thread_id", m.getThreadId());
        map.put("reply_count", m.getReplyCount());
        return map;
    }

    private String sanitize(String text) {
        if (text == null) return null;
        return text.strip().replace("<", "&lt;").replace(">", "&gt;");
    }
}
