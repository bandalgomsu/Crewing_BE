package com.crewing.file.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.crewing.club.entity.Club;
import com.crewing.file.entity.ClubFile;
import com.crewing.file.repository.ClubFileRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileServiceImpl implements FileService{
    // @Value는 lombok 어노테이션이 아님에 주의!
    // 버켓 이름 동적 할당(properties에서 가져옴)
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    @Value("${cloud.aws.s3.bucket.url}")
    private String bucketUrl;
    private final AmazonS3 amazonS3;
    private final ClubFileRepository fileRepository;

    @Override
    @Transactional
    public List<String> uploadMultiFile(List<MultipartFile> multipartFileList) throws IOException {
        List<String> fileList = new ArrayList<>();
        try {
            for (MultipartFile file : multipartFileList) {
                // 파일 이름의 중복을 막기 위해 "UUID(랜덤 값) + 원본파일이름"로 연결함
                String s3FileName = UUID.randomUUID() + "-" + file.getOriginalFilename();

                // 파일 사이즈를 ContentLength를 이용하여 S3에 알려줌
                ObjectMetadata objMeta = new ObjectMetadata();
                // url을 클릭 시 사진이 웹에서 보이는 것이 아닌 바로 다운되는 현상을 해결하기 위해 메타데이터 타입 설정
                objMeta.setContentType(file.getContentType());
                InputStream inputStream = file.getInputStream();
                objMeta.setContentLength(inputStream.available());

                // 파일 stream을 열어서 S3에 파일을 업로드
                amazonS3.putObject(bucket, s3FileName, inputStream, objMeta);
                inputStream.close();

                // Url 가져와서 반환
                log.info("S3 upload file name = {}", s3FileName);
                fileList.add(amazonS3.getUrl(bucket, s3FileName).toString());
            }
        }
        catch (AmazonS3Exception e) {
            throw new AmazonS3Exception("Failed to upload multiple files", e);
        }
        return fileList;
    }

    @Override
    @Transactional
    public String uploadFile(MultipartFile file) throws IOException {
        try {
            // 파일 이름의 중복을 막기 위해 "UUID(랜덤 값) + 원본파일이름"로 연결함
            String s3FileName = UUID.randomUUID() + "-" + file.getOriginalFilename();

            // 파일 사이즈를 ContentLength를 이용하여 S3에 알려줌
            ObjectMetadata objMeta = new ObjectMetadata();
            // url을 클릭 시 사진이 웹에서 보이는 것이 아닌 바로 다운되는 현상을 해결하기 위해 메타데이터 타입 설정
            objMeta.setContentType(file.getContentType());
            InputStream inputStream = file.getInputStream();
            objMeta.setContentLength(inputStream.available());

            // 파일 stream을 열어서 S3에 파일을 업로드
            amazonS3.putObject(bucket, s3FileName, inputStream, objMeta);
            inputStream.close();

            // Url 가져와서 반환
            log.info("S3 upload file name = {}", s3FileName);
            return amazonS3.getUrl(bucket, s3FileName).toString();
        }
        catch (AmazonS3Exception e) {
            throw new AmazonS3Exception("Failed to upload file", e);
        }
    }

    @Override
    @Transactional
    public void deleteMultiFile(List<String> fileUrlList) {
        try {
            for (String fileUrl : fileUrlList) {
                fileRepository.deleteAllByImageUrl(fileUrl);
                log.info("delete imgUrl = {}",fileUrl);
                String fileName = fileUrl.substring(bucketUrl.length() + 1);
                DeleteObjectRequest request = new DeleteObjectRequest(bucket, fileName);
                amazonS3.deleteObject(request);
            }
        }
        catch (AmazonS3Exception e) {
            throw new AmazonS3Exception("Failed to delete multiple files", e);
        }
    }

    @Override
    @Transactional
    public void deleteFile(String fileUrl) {
        try {
            fileRepository.deleteAllByImageUrl(fileUrl);
            String fileName = fileUrl.substring(bucketUrl.length() + 1);
            DeleteObjectRequest request = new DeleteObjectRequest(bucket, fileName);
            amazonS3.deleteObject(request);
        }catch (AmazonS3Exception e) {
            throw new AmazonS3Exception("Failed to delete file", e);
        }
    }

    @Override
    public List<ClubFile> createClubFile(Club club, List<String> fileUrl) {
        List<ClubFile> clubFileList = new ArrayList<>();
        for (String file : fileUrl) {
            ClubFile clubFile = ClubFile.builder()
                    .club(club)
                    .imageUrl(file)
                    .build();
            clubFileList.add(clubFile);
        }
        return fileRepository.saveAll(clubFileList);
    }

}
