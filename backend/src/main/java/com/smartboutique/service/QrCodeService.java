package com.smartboutique.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * Generation d'images QR Code (PNG) via ZXing.
 */
@Service
public class QrCodeService {

    private static final int DEFAULT_SIZE = 300;

    /** Genere l'image PNG du QR Code encodant le contenu fourni. */
    public byte[] generatePng(String content) {
        return generatePng(content, DEFAULT_SIZE);
    }

    public byte[] generatePng(String content, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, 1,
                    EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            // Echec de generation : erreur serveur (ne devrait pas arriver avec un contenu valide).
            throw new IllegalStateException("Echec de generation du QR Code", e);
        }
    }
}
