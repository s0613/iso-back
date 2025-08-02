package com.isoplatform.api.auth;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /** Auto-increment PK */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인용 이메일 */
    @Column(nullable = false, unique = true, length = 120)
    private String email;

    /** 이름 */
    @Column(nullable = false, length = 255)
    private String name;

    /** 비밀번호 */
    @Column(nullable = false, length = 255)
    private String password;

    /** 회사 */
    @Column(nullable = false, length = 255)
    private String company;

    /** 역할(Enum) */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    /** 활성 여부 · 마지막 로그인 */
    @Builder.Default private Boolean isActive = true;
    private LocalDateTime lastLoginAt;

}
