# 빅 데이터 처리 sns 

### 제작기간 : 24.01.09 ~ 

### 개발 스택 
1. Frontend : 
2. Backend : intelij
3. Database : mysql

1일차. 회원 등록 

기능 구현에 필요한 패키지 생성 - dto, entity, repository, service, controller, member, follow

### dto.RegisterMemberCommand

```
public record RegisterMemberCommand(
        // record - getter, setter가 자동으로 생성
        String email,
        String nickname,
        LocalDate birthday
) {
}
```
record클래스 : 순수하게 데이터를 보유하기 위한 특수한 종류의 클래스.<br>
특징으로는 <br><br>
1. final 클래스이라 상속할 수 없다.<br>
2. 각 필드는 private final 필드로 정의된다.<br>
3. 모든 필드를 초기화하는 RequiredAllArgument 생성자가 생성된다.<br>
4. 각 필드의 getter는 getXXX()가 아닌, 필드명을 딴 getter가 생성된다.(name(), age(), address())<br>
이중 4번을 유념하자.

---

### entity.Member
Member 객체를 생성하고 생성할 때 지켜야할 제약조건들을 검증하는 메서드를 추가
```
@Getter
public class Member {
    final private Long id;

    private String nickname;

    final private String email;

    final private LocalDate birthday;

    final private LocalDateTime createdAt; // 생성일시, 디버깅을 위해 생성

    final private static Long NAME_MAX_LENGTH = 10L;

    @Builder
    public Member(Long id, String nickname, String email, LocalDate birthday, LocalDateTime createdAt) {
        this.id = id; // 이후 JPA 사용을 위해 nullable하게 생성
        this.email = Objects.requireNonNull(email);
        this.birthday = Objects.requireNonNull(birthday);

        validateNickname(nickname);
        this.nickname = Objects.requireNonNull(nickname); //Objects.requireNonNull - not null 체크
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    void validateNickname(String nickname) {
        Assert.isTrue(nickname.length() <= NAME_MAX_LENGTH, "최대 길이를 초과했습니다.");
    }
}
```
@Builder패턴 사용 이유 
1. 필요한 데이터만 설정할 수 있음<br>
2. 유연성을 확보할 수 있음<br>
3. 가독성을 높일 수 있음<br>
4. 변경 가능성을 최소화할 수 있음<br>
Objects.requireNonNull()메서드를 통해 null 체크가 가능하다. 놀랍다.

---

### repository.MemberRepository
회원을 등록하는 구체적인 기능을 구현한다.

```
@RequiredArgsConstructor
@Repository
public class MemberRepository {
    final private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    public Member save(Member member) {
        /*
            member id를 보고 갱신 또는 삽입을 정함
            반환값은 id를 담아서 반환한다.
        */
        if(member.getId() == null) {
            return insert(member);
        }
        return update(member);
    }

    private Member insert(Member member) {
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(namedParameterJdbcTemplate.getJdbcTemplate())
                .withTableName("member") // ()에 테이블 이름 지정
                .usingGeneratedKeyColumns("id"); // ()에 GeneratedKey로 사용할 컬럼 지정
        SqlParameterSource params = new BeanPropertySqlParameterSource(member);
        var id = simpleJdbcInsert.executeAndReturnKey(params).longValue(); // simpleJdbcInsert.executeAndReturnKey(SqlParameterSource) 키를 반환 
        return member.builder()
                .id(id)
                .email(member.getEmail())
                .nickname(member.getNickname())
                .birthday(member.getBirthday())
                .createdAt(member.getCreatedAt())
                .build();
    }

    private Member update(Member member) {
        // TODO: implemented
        return member;
    }
}
```

JdbcTemplate을 처음 사용해봤다. <br>
spring-jdbc 라이브러리에 포함되어 있기 때문에 jdbc를 사용한다면 기본적으로 사용할 수 있고, 대부분의 반복 작업을 처리해주는 장점이 있지만
동적 쿼리문을 해결하기 어렵다는 단점 역시 존재한다. <br>
하지만 동적 쿼리문의 수월한 작성을 위해 다른 DB 접근 기술을 사용할 수 있다.
예를 들어 MyBatis와 JPA+QueryDSL 을 사용할 수 있다.

### service.MemberWriteService
회원을 등록하는 기능 추가 

```
@RequiredArgsConstructor
@Service
public class MemberWriteService {
    final private MemberRepository memberRepository;
    public Member create(RegisterMemberCommand command) {
        /*
            목표 - 회원정보(이메일, 닉네임, 생년월일)를 등록한다.
                - 닉네임은 10자를 넘길 수 없다.
            파라미터 - memberRegisterCommand
            val member = Member.of(memberRegisterCommand)
            memberRepository.save(member)
        */
        var member = Member.builder()
                .nickname(command.nickname())
                .email(command.email())
                .birthday(command.birthday())
                .build();
        return memberRepository.save(member);
    }
}
```

record 클래스를 사용했기 때문에 command.get변수 이 아니라 command.변수임을 확인할 수 있다. 
