package com.ass.br.apiradar.infrastructure.component;

import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.processing.CoverageProcessor;
import org.opengis.coverage.processing.Operation;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.springframework.stereotype.Component;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.OperationJAI;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

@Component
public class GeoToolsProcessor {

    public byte[] processar(byte[] imagemBruta) throws IOException, FactoryException {
        // Ler a imagem bruta como GeoTIFF
        GridCoverage2D cobertura = lerImagemGeoTIFF(imagemBruta);

        // Aplicar correção geométrica (reprojeção para EPSG:4326)
        cobertura = corrigirProjecao(cobertura);

        // Aplicar filtro de suavização (exemplo usando GaussianBlur)
        cobertura = aplicarFiltroSuavizacao(cobertura);

        // Exportar a imagem processada de volta para byte[]
        return salvarImagemGeoTIFF(cobertura);
    }

    private GridCoverage2D lerImagemGeoTIFF(byte[] imagemBruta) throws IOException {
        File tempFile = File.createTempFile("geotiff", ".tif");
        try {
            java.nio.file.Files.write(tempFile.toPath(), imagemBruta);
            GeoTiffReader reader = new GeoTiffReader(tempFile);
            try {
                return reader.read(null);
            } finally {
                reader.dispose();
            }
        } finally {
            tempFile.delete();
        }
    }

    private GridCoverage2D corrigirProjecao(GridCoverage2D cobertura) throws FactoryException {
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326", true);
        if (cobertura.getCoordinateReferenceSystem2D().equals(targetCRS)) {
            return cobertura;
        }

        CoverageProcessor processor = CoverageProcessor.getInstance();
        ParameterValueGroup parameters = processor.getOperation("Resample").getParameters();
        parameters.parameter("Source").setValue(cobertura);
        parameters.parameter("CoordinateReferenceSystem").setValue(targetCRS);

        return (GridCoverage2D) processor.doOperation(parameters);
    }

    private GridCoverage2D aplicarFiltroSuavizacao(GridCoverage2D cobertura) {
        ParameterBlockJAI parameters = new ParameterBlockJAI("GaussianBlur");
        parameters.setSource("source0", cobertura.getRenderedImage());
        parameters.setParameter("sigma", 5.0f);

        RenderedImage processedImage = JAI.create("GaussianBlur", parameters);

        return new GridCoverageFactory().create(
                cobertura.getName(),
                processedImage,
                cobertura.getEnvelope()
        );
    }

    private byte[] salvarImagemGeoTIFF(GridCoverage2D cobertura) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            GeoTiffWriter writer = new GeoTiffWriter(outputStream);
            writer.write(cobertura, null);
            writer.dispose();

            return outputStream.toByteArray();
        }
    }
}


