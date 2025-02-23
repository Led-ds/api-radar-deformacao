package com.ass.br.apiradar.infrastructure.component;

import com.ass.br.apiradar.domain.model.Deformacao;
import com.sun.media.jai.opimage.MedianFilterRIF;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.operation.MedianFilter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.springframework.stereotype.Component;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.imageio.ImageIO;
import javax.media.jai.operator.MedianFilterDescriptor;
import javax.media.jai.operator.MedianFilterShape;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

@Component
public class GeoToolsProcessor {

    public byte[] processar(byte[] imagemBruta, Deformacao deformacao) throws IOException, FactoryException {
        // Ler a imagem bruta como GeoTIFF
        GridCoverage2D cobertura = lerImagemGeoTIFF(imagemBruta, deformacao);

        // Aplicar correção geométrica (reprojeção para EPSG:4326)
        cobertura = corrigirProjecao(cobertura);

        // Aplicar filtro de suavização (exemplo usando GaussianBlur)
        cobertura = aplicarFiltroSuavizacao(cobertura);

        // Exportar a imagem processada de volta para byte[]
        return salvarImagemGeoTIFF(cobertura);
    }

    private GridCoverage2D lerImagemGeoTIFF(byte[] imagemBruta, Deformacao deformacao) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(imagemBruta);

        BufferedImage imagem = ImageIO.read(inputStream);

        if (imagem == null) {
            throw new IOException("Não foi possível ler a imagem PNG.");
        }

        System.setProperty("org.geotools.referencing.forceXY", "true"); // Força a ordem correta dos eixos
        ReferencingFactoryFinder.scanForPlugins(); // Garante que todas as fábricas sejam carregadas

        ReferencedEnvelope envelope = geoBoundingCalculator(deformacao);

        GridCoverageFactory gcf = new GridCoverageFactory();

        return gcf.create("ImagemGeorreferenciada", imagem, envelope);

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
        //ParameterBlockJAI parameters = new ParameterBlockJAI("GaussianBlur");
        //parameters.setSource("source0", cobertura.getRenderedImage());
        //parameters.setParameter("sigma", 5.0f);

        float sigma = 5.0f;
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(cobertura.getRenderedImage());
        pb.add(sigma);


        RenderedImage processedImage = null;

        try{
            processedImage = JAI.create("Log", pb); //, Exp, Scale, Multiply, Convolve, MultiplyConst, DivideByConst.
        }catch (IllegalArgumentException e){
            try {
                processedImage = JAI.create("Exp", pb);
            }catch (IllegalArgumentException ex1){
                try {
                    processedImage = JAI.create("Scale", pb);
                }catch (IllegalArgumentException ex2){
                    try {
                        processedImage = JAI.create("Multiply", pb);
                    }catch (IllegalArgumentException ex3){
                        try {
                            processedImage = JAI.create("Convolve", pb);
                        }catch (IllegalArgumentException ex4){
                            try {
                                processedImage = JAI.create("MultiplyConst", pb);
                            }catch (IllegalArgumentException ex5){
                                try {
                                    processedImage = JAI.create("DivideByConst", pb);
                                }catch (IllegalArgumentException ex6){
                                    throw new IllegalArgumentException();
                                }
                            }
                        }
                    }
                }
            }
        }
        //RenderedImage processedImage = JAI.create("UnsharpMask", pb); //Log, Exp, Scale, Multiply, Convolve, MultiplyConst, DivideByConst.

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

    private ReferencedEnvelope geoBoundingCalculator(final Deformacao deformacao){

        // Conversão metros -> graus
        double metrosPorGrauLat = 111320;
        double metrosPorGrauLon = 111320 * Math.cos(Math.toRadians(deformacao.getLatitude()));

        double deslocamentoGrausLat = deformacao.getDeslocamento() / metrosPorGrauLat;
        double deslocamentoGrausLon = deformacao.getDeslocamento() / metrosPorGrauLon;

        // Definir limites
        double minLat = deformacao.getLatitude() - deslocamentoGrausLat;
        double maxLat = deformacao.getLatitude() + deslocamentoGrausLat;

        double minLon = deformacao.getLongitude() - deslocamentoGrausLon;
        double maxLon = deformacao.getLongitude() + deslocamentoGrausLon;

        // Criar ReferencedEnvelope
        CoordinateReferenceSystem crs = null;
        try {
            crs = CRS.decode("EPSG:4326");

        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }
        ReferencedEnvelope envelope = new ReferencedEnvelope(minLon, maxLon, minLat, maxLat, crs);



        return envelope;
    }
}


