package com.ass.br.apiradar.domain.repositories;

import com.ass.br.apiradar.domain.model.Deformacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeformacaoRepository extends JpaRepository<Deformacao, Long> {
}

