package com.ass.br.apiradar.domain.repositories;

import com.ass.br.apiradar.domain.model.AnaliseResultado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnaliseResultadoRepository extends JpaRepository<AnaliseResultado, Long> {
}

