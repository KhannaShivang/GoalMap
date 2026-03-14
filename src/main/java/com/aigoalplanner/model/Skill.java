package com.aigoalplanner.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "skills")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 80)
    private String category;

    @OneToMany(mappedBy = "skill", fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private List<SkillDependency> dependencies = new ArrayList<>();

    @OneToMany(mappedBy = "dependsOn", fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private List<SkillDependency> dependents = new ArrayList<>();
}
