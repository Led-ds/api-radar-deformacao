package com.ass.br.apiradar.infrastructure.component;

import org.springframework.stereotype.Component;

@Component
public class CalculoGeoespacial {

    private static final double RAIO_TERRA_KM = 6371.0; // Raio médio da Terra em km

    /**
     * Calcula a distância geodésica entre dois pontos (lat/long) usando a fórmula de Haversine.
     * @param lat1 Latitude do primeiro ponto
     * @param lon1 Longitude do primeiro ponto
     * @param lat2 Latitude do segundo ponto
     * @param lon2 Longitude do segundo ponto
     * @return Distância em quilômetros
     */
    public double calcularDistanciaHaversine(double lat1, double lon1, double lat2, double lon2) {
        double latRad1 = Math.toRadians(lat1);
        double latRad2 = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(latRad1) * Math.cos(latRad2) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return RAIO_TERRA_KM * c; // Retorna distância em km
    }

    /**
     * Converte a distância de quilômetros para centímetros.
     * @param distanciaKm Distância em quilômetros
     * @return Distância em centímetros
     */
    public double converterKmParaCm(double distanciaKm) {
        return distanciaKm * 100000; // 1 km = 100.000 cm
    }

    /**
     * Método principal para calcular o deslocamento entre dois pontos geográficos.
     * @param coordenadas Array com duas coordenadas [{lat1, lon1}, {lat2, lon2}]
     * @return Deslocamento em centímetros
     */
    public double calcularDeslocamento(double[][] coordenadas) {
        if (coordenadas.length < 2) {
            throw new IllegalArgumentException("É necessário pelo menos dois pontos para calcular o deslocamento.");
        }

        double lat1 = coordenadas[0][0];
        double lon1 = coordenadas[0][1];
        double lat2 = coordenadas[1][0];
        double lon2 = coordenadas[1][1];

        double distanciaKm = calcularDistanciaHaversine(lat1, lon1, lat2, lon2);
        return converterKmParaCm(distanciaKm);
    }
}
