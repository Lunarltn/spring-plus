# 구현 단계
***
### 필수 기능
1. 코드 개선 퀴즈 - @Transactional의 이해 
2. 코드 개선 퀴즈 - JWT의 이해 
3. 코드 개선 퀴즈 - JPA의 이해
4. 코드 개선 퀴즈 - 컨트롤러 테스트의 이해
5. 코드 개선 퀴즈 - AOP의 이해
6. JPA Cascade
7. N+1
8. QueryDSL
9. Spring Security

### 도전 기능
10. QueryDSL 을 사용하여 검색 기능 만들기
11. Transaction 심화

## 필수 기능
***
### 1. 코드 개선 퀴즈 - @Transactional의 이해 
* 해당 에러 해결하기
```
jakarta.servlet.ServletException: Request processing failed: 
org.springframework.orm.jpa.JpaSystemException: could not execute statement 
[Connection is read-only. Queries leading to data modification are not allowed] 
[insert into todos (contents,created_at,modified_at,title,user_id,weather) values (?,?,?,?,?,?)]
```
* 클래스에 `@Transactional(readOnly = true)` 설정이 되어있어서 해당 오류가 발생한걸로 판단하고 메서드에 새로 `@Transaction`을 붙혀 설정을 덮어씌웠다.

* [Git](https://github.com/Lunarltn/spring-plus/commit/510272090e37f557e75e43b64f9f15935d16fc27)

***

### 2. 코드 추가 퀴즈 - JWT의 이해
* User 엔티티에 nickname 컬럼 추가하기
* JWT 토큰에 nickname이 추가되어야 함으로 `JwtUtil.class`에서
`Jwts.builder().claim("nickname", nickname)`를 추가했고
`JwtFilter.class`에선 로컬에 사용될 계정 정보들을
`httpRequest.setAttribute("nickname", claims.get("nickname"));`으로 `HttpServletRequest`에 저장해뒀다.

* [Git](https://github.com/Lunarltn/spring-plus/commit/6a86a09fa582cddd684e9b39547bf5544a3d2db9)

***

### 3. 코드 개선 퀴즈 -  JPA의 이해
* 할 일 검색 시 `weather`과 수정일 기준 기간 검색 기능 추가하기
* JPQL을 사용해 해결했다.
```java
@Query("SELECT t FROM Todo t "+
       "LEFT JOIN FETCH t.user "+
       "WHERE (:weather IS NULL OR t.weather LIKE CONCAT('%', :weather, '%')) AND "+
       "(:start_date IS NULL OR t.modifiedAt >= :start_date) AND "+
       "(:end_date IS NULL OR t.modifiedAt <= :end_date ) "+
       "ORDER BY t.modifiedAt DESC")
```

* [Git](https://github.com/Lunarltn/spring-plus/commit/b02becd2102ebd72bdd83d0c2573e1bf88f3bb8c)

***

### 4. 테스트 코드 퀴즈 - 컨트롤러 테스트의 이해
* 테스트 패키지 `org.example.expert.domain.todo.controller`의 
`todo_단건_조회_시_todo가_존재하지_않아_예외가_발생한다()` 테스트 수정하기
* 예외 시 예상되는 HttpStatus는 `OK`가 아닌 `BAD_REQUEST`였기 때문에 이 부분을 수정했다.

* [Git](https://github.com/Lunarltn/spring-plus/commit/4ff800d8661641e116b1383907e8ef16753a8498)

***

### 5. 코드 개선 퀴즈 - AOP의 이해
* `AdminAccessLoggingAspect` AOP가 `UserAdminController` 클래스의 `changeUserRole()` 메소드가 실행 전 동작할 수 있도록 수정하기

```
@After("execution(* org.example.expert.domain.user.controller.UserController.getUser(..))")
```
* `UserController.getUser()` 함수가 실행 후 동작하는 코드이므로
```
@Before("execution(* org.example.expert.domain.user.controller.UserAdminController.changeUserRole(..))")
```
* `UserAdminController.changeUserRole()` 함수가 실행 되기 전 동작하는 코드로 변경했다.

* [Git](https://github.com/Lunarltn/spring-plus/commit/77e169b8d5399ce4635d8e3eebb6cc974bbaebbb)

***

### 6. JPA Cascade
* 할 일을 새로 저장할 시, 할 일을 생성한 유저는 담당자로 자동 등록되게 수정하기
(cascade를 활용해 할 일을 생성한 유저가 담당자 테이블이 등록될 수 있게 하기)
```java
@OneToMany(mappedBy = "todo")
private List<Manager> managers = new ArrayList<>();

public Todo(String title, String contents, String weather, User user) {
		...
        this.managers.add(new Manager(user, this));
    }
```
* 원본 코드의 Todo 생성자에서 할 일이 등록될 때, User와 Todo를 받아 담당자 엔티티를 새로 생성했는데 영속성 전이가 되지 않아 담당자 테이블에 엔티티가 추가 되지 않았다.
```
@OneToMany(mappedBy = "todo", cascade = CascadeType.PERSIST)
private List<Manager> managers = new ArrayList<>();
```
* `cascade = CascadeType.PERSIST`옵션을 추가해 저장 상태에 대한 영속성 전이를 가능하게 했다.

* [Git](https://github.com/Lunarltn/spring-plus/commit/d5e19f84b3ef60f981e3a267efe8e57bd5b6686a)

***

### 7. N+1
* `CommentController` 클래스의 `getComments()` API를 호출할 때 N+1 문제가 발생하지 않게 수정하기
```java
@Query("SELECT c FROM Comment c JOIN c.user WHERE c.todo.id = :todoId")
List<Comment> findByTodoIdWithUser(@Param("todoId") Long todoId);
```
* 해당 JPQL에서 `User`에 대한 Join만 했는데, 이렇게 불러온 엔티티가 LAZY 설정일 경우 해당 엔티티는 실제 객체가 아닌 프록시 객체만 가져와지므로 엔티티 내부 데이터를 호출 할 시 추가 쿼리가 발생한다. 
(실제 객체가 필요하기 때문)
```java
@Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.todo.id = :todoId")
```
* `JOIN FETCH`를 걸어줌으로 조회 시 프록시 객체가 아닌 실제 객체를 불러올 수 있도록 수정했다.

* [Git](https://github.com/Lunarltn/spring-plus/commit/7ac8ad187df4efb3202bf9ace55bdc969d9c216d)

***

### 8. QueryDSL
* JPQL로 작성된 `findByIdWithUser`를 QueryDSL로 변경하고 N+1 문제 해결하기
```java
@Query("SELECT t FROM Todo t " +
       "LEFT JOIN t.user " +
       "WHERE t.id = :todoId")
Optional<Todo> findByIdWithUser(@Param("todoId") Long todoId);
```
* 위의 JPQL로 작성된 쿼리문을 밑의 QueryDSL로 변경하고 `fetchJoin()`을 붙혀 user 조회 시 추가 발생할 쿼리를 방지했다.
```java
public Optional<Todo> findByIdWithUser(Long todoId) {
	return Optional.ofNullable(
		jpaQueryFactory
		.selectFrom(todo)
		.leftJoin(todo.user, user).fetchJoin()
		.where(todo.id.eq(todoId))
		.fetchOne());
}
```
* [Git](https://github.com/Lunarltn/spring-plus/commit/2d68b6be103efed10d335a3a90a2570f5faba583)
***

### 9. Spring Security
* 기존 `Filter`와 `Argument Resolver`를 사용해 인증/인가하던 코드를 Spring Security로 변경하기
* `@EnableWebSecurity`어노테이션이 붙은 Config를 생성해 Spring Security를 활성화 시키고, 기존 `HttpServletRequest`를 통해 전달받던 사용자 정보를 `SecurityContextHolder`를 사용하여 파라미터 전달 없이 어디서든 `SecurityContextHolder.getContext().getAuthentication()`를 통해 정보를 받아올 수 있게 했다.
* [Git](https://github.com/Lunarltn/spring-plus/commit/07ff5da8fb47006846160ed7291c3b4befd2c5ab)
***

### 10. QueryDSL 을 사용하여 검색 기능 만들기
* QueryDSL을 사용해 일정을 검색하는 기능을 만들고, 결과값을 페이징 처리와 `Projections`을 활용해 필요한 필드만 반환하도록 하기.
    ```java
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
    ```  
* [Git](https://github.com/Lunarltn/spring-plus/commit/5ba438c6fe4bda6640303d151fd85bc746862045)
***

### 11. Transaction 심화
* 매니저 등록 요청 시 로그를 저장하고, 요청이 실패해도 로그가 저장될 수 있도록 `@Transaction`을 설정하기
* 매니저를 등록하는 서비스를 try-catch로 감싸 내부에서 예외가 발생할 때 catch를 통해 예외 메시지를 저장할 수 있도록 구현했고, 런타임 예외 발생 시 롤백되는 트랜잭션의 특성을 `@Transactional(propagation = Propagation.REQUIRES_NEW)` 처리해서 별개의 동작을 할 수 있도록 설정했다.
* [Git](https://github.com/Lunarltn/spring-plus/commit/ef1dff4a1ca12d932c64990d8ec48e9707e0339e)
***
# 기술 정리
* [QueryDSL](https://velog.io/@qpsrltn/QueryDSL)
