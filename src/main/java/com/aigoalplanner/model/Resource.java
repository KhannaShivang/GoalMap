package com.aigoalplanner.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "resources")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String link;

    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ResourceType type = ResourceType.ARTICLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    @ToString.Exclude
    private Skill skill;

    @Column(length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Difficulty difficulty = Difficulty.BEGINNER;

    // stored as float[] — pgvector column is vector(768)
    @Column(name = "embedding", columnDefinition = "vector(768)")
    private float[] embedding;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum ResourceType {
        ARTICLE, COURSE, VIDEO, DOCUMENTATION, PROJECT
    }

    public enum Difficulty {
        BEGINNER, INTERMEDIATE, ADVANCED
    }
}
