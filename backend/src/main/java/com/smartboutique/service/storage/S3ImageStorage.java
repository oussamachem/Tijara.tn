package com.smartboutique.service.storage;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

/**
 * Stockage des images sur un objet-store S3 (SDK AWS v2). Endpoint configurable :
 *   - vrai <b>AWS S3</b> (endpoint vide) ;
 *   - <b>MinIO</b> auto-hébergé (endpoint http://minio:9000 + path-style).
 * Actif uniquement si {@code app.storage=s3}. Les URLs restent {@code /uploads/<uuid>.<ext>}
 * (servies par {@code UploadsController} qui relit l'objet).
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage", havingValue = "s3")
public class S3ImageStorage implements ImageStorage {

    @Value("${app.s3.endpoint:}")   private String endpoint;
    @Value("${app.s3.bucket}")      private String bucket;
    @Value("${app.s3.region:us-east-1}") private String region;
    @Value("${app.s3.access-key}")  private String accessKey;
    @Value("${app.s3.secret-key}")  private String secretKey;
    @Value("${app.s3.path-style:true}") private boolean pathStyle;

    private S3Client s3;

    @PostConstruct
    void init() {
        S3ClientBuilder b = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build());
        if (endpoint != null && !endpoint.isBlank()) {
            b.endpointOverride(URI.create(endpoint));   // MinIO / S3 compatible
        }
        this.s3 = b.build();
        ensureBucket();
        log.info("[storage=s3] bucket='{}' endpoint='{}' region='{}'", bucket,
                endpoint == null || endpoint.isBlank() ? "AWS" : endpoint, region);
    }

    private void ensureBucket() {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (S3Exception e) {
            try {
                s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                log.info("[storage=s3] bucket '{}' créé.", bucket);
            } catch (S3Exception ce) {
                log.warn("[storage=s3] bucket '{}' non créé ({}). Créez-le manuellement si besoin.", bucket, ce.awsErrorDetails() != null ? ce.awsErrorDetails().errorCode() : ce.getMessage());
            }
        }
    }

    @Override
    public String store(MultipartFile file) {
        String ext = validate(file);
        String key = UUID.randomUUID() + "." + ext;
        try {
            byte[] bytes = file.getBytes();
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucket).key(key)
                            .contentType(file.getContentType())
                            .contentLength((long) bytes.length)
                            .build(),
                    RequestBody.fromBytes(bytes));
        } catch (IOException | S3Exception e) {
            throw new IllegalStateException("Echec de l'enregistrement de l'image sur S3", e);
        }
        log.info("[storage=s3] Image enregistrée : {}", key);
        return "/uploads/" + key;
    }

    @Override
    public void delete(String url) {
        if (url == null || !url.startsWith("/uploads/")) return;
        String key = url.substring("/uploads/".length());
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
            log.info("[storage=s3] Image supprimée : {}", key);
        } catch (S3Exception e) {
            log.warn("[storage=s3] Echec suppression {} : {}", key, e.getMessage());
        }
    }

    @Override
    public LoadedImage load(String filename) {
        try {
            ResponseBytes<GetObjectResponse> obj = s3.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(filename).build());
            String ct = obj.response().contentType();
            if (ct == null || ct.isBlank()) ct = ImageStorage.contentTypeFor(filename);
            return new LoadedImage(obj.asByteArray(), ct);
        } catch (NoSuchKeyException e) {
            return null;
        } catch (S3Exception e) {
            log.warn("[storage=s3] Echec lecture {} : {}", filename, e.getMessage());
            return null;
        }
    }
}
