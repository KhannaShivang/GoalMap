package com.aigoalplanner.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "skill_dependencies",
       uniqueConstraints = @UniqueConstraint(columnNames = {"skill_id", "depends_on_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SkillDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    @ToString.Exclude
    private Skill skill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depends_on_id", nullable = false)
    @ToString.Exclude
    private Skill dependsOn;
}
