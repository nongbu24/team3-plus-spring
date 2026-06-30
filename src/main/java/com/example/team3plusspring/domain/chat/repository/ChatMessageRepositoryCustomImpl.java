package com.example.team3plusspring.domain.chat.repository;

import com.example.team3plusspring.domain.chat.entity.ChatMessage;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.example.team3plusspring.domain.chat.entity.QChatMessage.chatMessage;

@RequiredArgsConstructor
public class ChatMessageRepositoryCustomImpl implements ChatMessageRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<ChatMessage> findRecent(Pageable pageable) {
        return queryFactory
                .selectFrom(chatMessage)
                .join(chatMessage.chatRoom).fetchJoin()
                .orderBy(chatMessage.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public List<ChatMessage> findBefore(Long roomId, Long messageId, Pageable pageable) {
        return findByRoom(roomId, chatMessage.id.lt(messageId), chatMessage.id.desc(), pageable);
    }

    @Override
    public List<ChatMessage> findAfter(Long roomId, Long messageId, Pageable pageable) {
        return findByRoom(roomId, chatMessage.id.gt(messageId), chatMessage.id.asc(), pageable);
    }

    @Override
    public List<ChatMessage> findRecentByRoom(Long roomId, Pageable pageable) {
        return findByRoom(roomId, null, chatMessage.id.desc(), pageable);
    }

    private List<ChatMessage> findByRoom(
            Long roomId,
            BooleanExpression cursorCondition,
            OrderSpecifier<Long> order,
            Pageable pageable
    ) {
        return queryFactory
                .selectFrom(chatMessage)
                .where(
                        chatMessage.chatRoom.id.eq(roomId),
                        cursorCondition
                )
                .orderBy(order)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }
}
