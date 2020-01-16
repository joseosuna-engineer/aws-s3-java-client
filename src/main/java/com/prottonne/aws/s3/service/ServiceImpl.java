package com.prottonne.aws.s3.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.IOUtils;
import com.prottonne.aws.s3.service.dto.Request;
import com.prottonne.aws.s3.service.dto.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ServiceImpl {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String JPEG = "jpeg";
    private final String JPG = "jpg";
    private final String PNG = "png";

    @Value("${aws.s3.bucket.name}")
    private String s3BucketName;

    @Value("${folder}")
    private String folder;

    @Value("${aws.s3.bucket.name.general}")
    private String s3GeneralBucketName;

    @Autowired
    private AmazonS3 amazonS3;

    public ServiceImpl() {
        super();
    }

    public Response put(Request request) throws IOException {

        final String path = getPath();

        final String fileName = request.getFileName();

        final String key = getKey(path, fileName);

        final String fileMediaType = request.getFileMediaType();

        switch (fileMediaType) {
            case JPEG:
            case JPG:
            case PNG:
                request.setBase64(
                        compressImage(
                                request.getBase64(),
                                fileMediaType,
                                fileName
                        )
                );

        }

        byte[] fileInBytes = Base64.decodeBase64(request.getBase64());

        InputStream inputStream = new ByteArrayInputStream(fileInBytes);
        byte[] contentBytes = IOUtils.toByteArray(inputStream);
        Long contentLength = Long.valueOf(contentBytes.length);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);

        logger.info("uploading file");
        logger.info("key={}", key);
        logger.info("contentLength={}", contentLength);

        amazonS3.putObject(
                new PutObjectRequest(
                        s3BucketName,
                        key,
                        new ByteArrayInputStream(fileInBytes),
                        metadata
                )
        );

        return new Response();
    }

    public Response get(Request request, HttpServletResponse httpServletResponse) throws IOException {

        final String path = getPath();

        final String fileName = request.getFileName();

        final String key = getKey(path, fileName);

        S3Object s3Object = amazonS3.getObject(
                new GetObjectRequest(
                        s3BucketName,
                        key
                )
        );

        String contentType = s3Object.getObjectMetadata().getContentType();

        logger.info("contentType={}", contentType);

        InputStream s3ObjectContent = s3Object.getObjectContent();
        IOUtils.copy(s3ObjectContent, httpServletResponse.getOutputStream());

        httpServletResponse.setContentType("application/force-download");
        httpServletResponse.setHeader("Content-Disposition", "attachment; filename=" + s3Object.getKey());
        httpServletResponse.flushBuffer();

        s3ObjectContent.close();

        return new Response();
    }

    public Response deleteAll(Request request) {

        final String path = getPath();

        final String fileName = request.getFileName();

        final String key = getKey(path, fileName);

        ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
        listObjectsRequest.setBucketName(s3BucketName);
        listObjectsRequest.setPrefix(key);
        listObjectsRequest.setDelimiter("/");

        ObjectListing objectListing = amazonS3.listObjects(listObjectsRequest);
        logger.info("objectListing from={}", objectListing.getBucketName());

        List<S3ObjectSummary> s3ObjectSummaryList = objectListing.getObjectSummaries();
        logger.info("s3ObjectSummaryList={}", s3ObjectSummaryList);

        for (S3ObjectSummary s3ObjectSummary : s3ObjectSummaryList) {
            logger.info("deleting={}", s3ObjectSummary.getKey());
            amazonS3.deleteObject(
                    new DeleteObjectRequest(
                            s3BucketName,
                            s3ObjectSummary.getKey()
                    )
            );
        }

        return new Response();
    }

    public Response isUploaded(Request request) {

        Response response = new Response();

        final String path = getPath();

        final String fileName = request.getFileName();

        final String key = getKey(path, fileName);

        ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
        listObjectsRequest.setBucketName(s3BucketName);
        listObjectsRequest.setPrefix(key);
        listObjectsRequest.setDelimiter("/");

        ObjectListing objectListing = amazonS3.listObjects(listObjectsRequest);
        logger.info("objectListing from={}", objectListing.getBucketName());

        List<S3ObjectSummary> s3ObjectSummaryList = objectListing.getObjectSummaries();
        logger.info("s3ObjectSummaryList={}", s3ObjectSummaryList);

        for (S3ObjectSummary s3ObjectSummary : s3ObjectSummaryList) {
            logger.info("searching={} on={}", key, s3ObjectSummary.getKey());

            if (s3ObjectSummary.getKey().contains(key)) {
                response.setUploaded(Boolean.TRUE);
                return response;
            }
        }

        response.setUploaded(Boolean.FALSE);
        return response;
    }

    private String getPath() {

        return s3GeneralBucketName
                + "/"
                + folder;
    }

    private String getKey(String path, String fileName) {
        return path + "/" + fileName;
    }

    private String compressImage(String base64, String fileMediaType, String fileName) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
