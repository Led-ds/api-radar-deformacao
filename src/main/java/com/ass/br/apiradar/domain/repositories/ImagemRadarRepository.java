package com.ass.br.apiradar.domain.repositories;

import com.ass.br.apiradar.domain.model.ImagemRadar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImagemRadarRepository extends JpaRepository<ImagemRadar, Long> {
    List<ImagemRadar> findByDeformacaoId(Long deformacaoId);
}

