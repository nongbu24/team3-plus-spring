package com.example.team3plusspring.domain.point.entity;

import com.example.team3plusspring.domain.user.entity.User;
import com.example.team3plusspring.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "point_accounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private Long balance;

    private PointAccount(User user) {
        this.user = user;
        this.balance = 0L;
    }

    public static PointAccount create(User user) {
        return new PointAccount(user);
    }
}
