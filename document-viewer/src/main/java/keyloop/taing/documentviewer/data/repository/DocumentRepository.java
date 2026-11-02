package keyloop.taing.documentviewer.data.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import keyloop.taing.documentviewer.data.DocumentSnapshot;
import keyloop.taing.documentviewer.data.dto.Source;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentSnapshot, Long> {
    Optional<DocumentSnapshot> findByVinAndSource(String vin, Source source);
}
