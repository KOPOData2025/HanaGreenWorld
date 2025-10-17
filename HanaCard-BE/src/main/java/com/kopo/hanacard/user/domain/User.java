package com.kopo.hanacard.user.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_seq")
    @SequenceGenerator(name = "users_seq", sequenceName = "USERS_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "birth_date", nullable = false)
    private String birthDate;

    @Column(name = "address")
    private String address;

    @Column(name = "customer_grade", length = 20)
    private String customerGrade;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "ci", unique = true, length = 32)
    private String ci;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public User(String username, String email, String phoneNumber, String name, 
                String birthDate, String address, String customerGrade, Boolean isActive, String ci, LocalDateTime createdAt) {
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.name = name;
        this.birthDate = birthDate;
        this.address = address;
        this.customerGrade = customerGrade;
        this.isActive = isActive != null ? isActive : true;
        this.ci = ci;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public void updateUserInfo(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public void deactivate() {
        this.isActive = false;
    }
}




