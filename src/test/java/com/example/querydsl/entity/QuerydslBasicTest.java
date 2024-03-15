package com.example.querydsl.entity;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.querydsl.entity.QMember.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory factory = new JPAQueryFactory(em);
    @BeforeEach
    public void before(){
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 11, teamA);

        Member member3 = new Member("member3", 12, teamB);
        Member member4 = new Member("member4", 13, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL(){
        //member1을 찾아라
        //런타임 오류로 실수를 알게됨
        String qlString =
                "select m from Member m " +
                "where m.username = :username";

        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username","member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl(){
        //컴파일 오류로 알게됨

         Member findMember = factory.select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

         assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search(){
        Member findMember = factory.selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam(){
        Member findMember = factory.selectFrom(member)
                .where(member.username.eq("member1"), //and인경우 넘김
                        (member.age.eq(10))
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() {
        List<Member> fetch = factory.selectFrom(member).fetch(); //리스트 조회

        Member fetchOne = factory.selectFrom(member).fetchOne(); //단건 조회

        Member fetchFirst = factory.selectFrom(member).fetchFirst(); //단건 조회

        QueryResults<Member> results = factory.selectFrom(member)
                .fetchResults(); //페이징 정보 포함, totalCount 쿼리 추가 실행

        results.getTotal();
        List<Member> content = results.getResults();

        long total = factory.selectFrom(member)
                .fetchCount(); //count쿼리로 변경해서 count수 조회

    }

    /**
     * 회원 정렬 순서
     * 1.회원 나이 내림차순 (desc)
     * 2.회원 이름 올림차순 (asc)
     * 단 2에서
     */
    @Test
    public void sort(){
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = factory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2); //null은 마지막에

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1(){
        List<Member> result = factory.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //1부터
                .limit(2) //2개 가져옴
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2(){
        QueryResults<Member> result = factory.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //1부터
                .limit(2) //2개 가져옴
                .fetchResults();

        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(result.getLimit()).isEqualTo(2);
        assertThat(result.getOffset()).isEqualTo(1);
        assertThat(result.getResults()).isEqualTo(2);
    }

}
