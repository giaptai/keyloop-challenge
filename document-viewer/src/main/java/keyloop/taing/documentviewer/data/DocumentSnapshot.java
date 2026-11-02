package keyloop.taing.documentviewer.data;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import keyloop.taing.documentviewer.data.dto.Source;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "document_snapshot", uniqueConstraints = @UniqueConstraint(name = "uk_snapshot_vin_source", columnNames = {
        "vin", "source" }))
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    String vin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Source source;

    @Column(nullable = false, columnDefinition = "text")
    String payload;

    @Column(nullable = false)
    LocalDateTime fetchedAt;
}
