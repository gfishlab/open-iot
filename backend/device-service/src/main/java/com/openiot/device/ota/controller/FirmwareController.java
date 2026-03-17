package com.openiot.device.ota.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openiot.common.core.result.ApiResponse;
import com.openiot.device.ota.entity.FirmwareVersion;
import com.openiot.device.ota.service.FirmwareService;
import com.openiot.device.ota.vo.FirmwareUploadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 固件管理控制器
 * 提供固件上传、列表查询、文件下载（支持断点续传）等接口
 *
 * @author open-iot
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ota/firmware")
@RequiredArgsConstructor
@Tag(name = "OTA 固件管理", description = "固件上传、查询、下载接口")
public class FirmwareController {

    private final FirmwareService firmwareService;

    /**
     * 上传固件
     */
    @PostMapping
    @Operation(summary = "上传固件", description = "上传固件文件并创建版本记录")
    public ApiResponse<FirmwareVersion> upload(
            @Valid FirmwareUploadVO vo,
            @Parameter(description = "固件文件") @RequestPart(name = "file") MultipartFile file) {

        log.info("上传固件: productId={}, name={}, version={}, fileSize={}",
                vo.getProductId(), vo.getFirmwareName(), vo.getVersion(), file.getSize());

        FirmwareVersion firmware = firmwareService.upload(vo, file);
        return ApiResponse.success("固件上传成功", firmware);
    }

    /**
     * 分页查询固件列表
     */
    @GetMapping
    @Operation(summary = "查询固件列表", description = "按产品分页查询固件版本")
    public ApiResponse<Page<FirmwareVersion>> list(
            @Parameter(description = "产品ID") @RequestParam(name = "productId", required = false) Long productId,
            @Parameter(description = "页码") @RequestParam(name = "page", defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(name = "size", defaultValue = "10") int size) {

        log.info("查询固件列表: productId={}, page={}, size={}", productId, page, size);

        Page<FirmwareVersion> result = firmwareService.listByProduct(productId, page, size);
        return ApiResponse.success(result);
    }

    /**
     * 下载固件文件（支持 HTTP Range 断点续传）
     */
    @GetMapping("/{firmwareId}/download")
    @Operation(summary = "下载固件", description = "下载固件文件，支持 Range 断点续传")
    public ResponseEntity<Resource> download(
            @Parameter(description = "固件ID") @PathVariable(name = "firmwareId") Long firmwareId,
            @RequestHeader(name = HttpHeaders.RANGE, required = false) String rangeHeader) {

        log.info("下载固件: firmwareId={}, range={}", firmwareId, rangeHeader);

        String filePath = firmwareService.getFilePath(firmwareId);
        Path path = Paths.get(filePath);
        String fileName = path.getFileName().toString();

        try {
            long fileSize = Files.size(path);

            // 无 Range 请求头：返回完整文件
            if (rangeHeader == null || rangeHeader.isEmpty()) {
                FileSystemResource resource = new FileSystemResource(path);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .contentLength(fileSize)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .body(resource);
            }

            // 解析 Range 头：支持 bytes=start-end 格式
            String rangeValue = rangeHeader.replace("bytes=", "");
            String[] ranges = rangeValue.split("-");
            long rangeStart = Long.parseLong(ranges[0]);
            long rangeEnd = ranges.length > 1 && !ranges[1].isEmpty()
                    ? Long.parseLong(ranges[1])
                    : fileSize - 1;

            // 校验范围合法性
            if (rangeStart > rangeEnd || rangeStart >= fileSize) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                        .build();
            }

            long contentLength = rangeEnd - rangeStart + 1;

            // 读取指定范围的字节数据
            byte[] data = new byte[(int) contentLength];
            try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
                raf.seek(rangeStart);
                raf.readFully(data);
            }

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(contentLength)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_RANGE, "bytes " + rangeStart + "-" + rangeEnd + "/" + fileSize)
                    .body(new org.springframework.core.io.ByteArrayResource(data));

        } catch (IOException e) {
            log.error("固件下载失败: firmwareId={}, error={}", firmwareId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
