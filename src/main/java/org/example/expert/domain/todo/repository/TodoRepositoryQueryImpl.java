package org.example.expert.domain.todo.repository;

import static org.example.expert.domain.todo.entity.QTodo.todo;
import static org.example.expert.domain.user.entity.QUser.user;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.expert.domain.todo.dto.response.TodoSearchResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class TodoRepositoryQueryImpl implements TodoRepositoryQuery {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Optional<Todo> findByIdWithUser(Long todoId) {
        return Optional.ofNullable(
                jpaQueryFactory
                        .selectFrom(todo)
                        .leftJoin(todo.user, user).fetchJoin()
                        .where(todo.id.eq(todoId))
                        .fetchOne());
    }

    @Override
    public Page<TodoSearchResponse> searchTodos(
            String title,
            LocalDate startDate,
            LocalDate endDate,
            String nickname,
            Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        if(title != null && !title.isEmpty()) {
            builder.and(todo.title.containsIgnoreCase(title));
        }
        if(startDate != null && endDate != null) {
            builder.and(todo.createdAt.between(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX)));
        } else if(startDate != null) {
            builder.and(todo.createdAt.after(startDate.atStartOfDay()));
        } else if(endDate != null) {
            builder.and(todo.createdAt.before(endDate.atTime(LocalTime.MAX)));
        }
        if(nickname != null && !nickname.isEmpty()) {
            builder.and(todo.user.nickname.containsIgnoreCase(nickname));
        }

        List<TodoSearchResponse> list = jpaQueryFactory
                .select(Projections.constructor(
                        TodoSearchResponse.class,
                        todo.title,
                        todo.managers.size(),
                        todo.comments.size()
                ))
                .from(todo)
                .where(builder)
                .orderBy(todo.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> totalQuery = jpaQueryFactory
                .select(todo.count())
                .from(todo);

        return PageableExecutionUtils.getPage(list, pageable, totalQuery::fetchOne);
    }
}
