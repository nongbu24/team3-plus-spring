package com.example.team3plusspring.domain.chat.repository;

import com.example.team3plusspring.domain.chat.entity.ChatMessage;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.example.team3plusspring.domain.chat.entity.QChatMessage.chatMessage;

@RequiredArgsConstructor
public class ChatMessageRepositoryCustomImpl implements ChatMessageRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<ChatMessage> findRecentMessages(Pageable pageable) {
        return queryFactory
                .selectFrom(chatMessage)
                .orderBy(chatMessage.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public List<ChatMessage> findMessagesBeforeByRoom(Long roomId, Long lastMessageId, Pageable pageable) {
        return queryFactory
                .selectFrom(chatMessage)
                .where(
                        chatMessage.chatRoom.id.eq(roomId),
                        chatMessage.id.lt(lastMessageId)
                )
                .orderBy(chatMessage.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public List<ChatMessage> findMessagesAfterByRoom(Long roomId, Long lastMessageId, Pageable pageable) {
        return queryFactory
                .selectFrom(chatMessage)
                .where(
                        chatMessage.chatRoom.id.eq(roomId),
                        chatMessage.id.gt(lastMessageId)
                )
                .orderBy(chatMessage.id.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public List<ChatMessage> findRecentByRoom(Long roomId, Pageable pageable) {
        return queryFactory
                .selectFrom(chatMessage)
                .where(chatMessage.chatRoom.id.eq(roomId))
                .orderBy(chatMessage.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }
}
