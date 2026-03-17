package com.openiot.device.ota.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.common.security.context.TenantContext;
import com.openiot.device.ota.entity.FirmwareVersion;
import com.openiot.device.ota.mapper.FirmwareVersionMapper;
import com.openiot.device.ota.vo.FirmwareUploadVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

/**
 * 固件管理服务
 * 负责固件版本的上传、查询、下载路径获取等操作
 *
 * @author open-iot
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FirmwareService extends ServiceImpl<FirmwareVersionMapper, FirmwareVersion> {

    /** 固件文件存储根目录 */
    private static final String FIRMWARE_BASE_DIR = "data/firmware";

    /**
     * 上传固件
     * 将固件文件保存到本地文件系统，计算校验和并创建数据库记录
     *
     * @param vo   固件上传参数
     * @param file 固件文件
     * @return 创建的固件版本记录
     */
    @Transactional(rollbackFor = Exception.class)
    public FirmwareVersion upload(FirmwareUploadVO vo, MultipartFile file) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw BusinessException.unauthorized("租户信息不存在");
        }

        // 检查同一产品下版本号是否重复
        long existCount = lambdaQuery()
                .eq(FirmwareVersion::getTenantId, Long.valueOf(tenantId))
                .eq(FirmwareVersion::getProductId, vo.getProductId())
                .eq(FirmwareVersion::getVersion, vo.getVersion())
                .count();
        if (existCount > 0) {
            throw BusinessException.badRequest("该产品下版本号 " + vo.getVersion() + " 已存在");
        }

        // 构建存储路径：data/firmware/{tenantId}/{productId}/{version}/
        String relativePath = tenantId + "/" + vo.getProductId() + "/" + vo.getVersion();
        Path dirPath = Paths.get(FIRMWARE_BASE_DIR, relativePath);
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "firmware.bin";
        Path filePath = dirPath.resolve(originalFilename);

        try {
            // 创建目录并保存文件
            Files.createDirectories(dirPath);
            try (InputStream is = file.getInputStream()) {
                Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("固件文件保存成功: path={}", filePath);
        } catch (IOException e) {
            throw BusinessException.internalError("固件文件保存失败: " + e.getMessage());
        }

        // 计算 MD5 和 SHA256 校验和
        String md5 = computeHash(filePath, "MD5");
        String sha256 = computeHash(filePath, "SHA-256");

        // 创建数据库记录
        FirmwareVersion firmware = new FirmwareVersion();
        firmware.setTenantId(Long.valueOf(tenantId));
        firmware.setProductId(vo.getProductId());
        firmware.setFirmwareName(vo.getFirmwareName());
        firmware.setVersion(vo.getVersion());
        firmware.setFilePath(filePath.toString().replace("\\", "/"));
        firmware.setFileSize(file.getSize());
        firmware.setFileMd5(md5);
        firmware.setFileSha256(sha256);
        firmware.setDescription(vo.getDescription());
        firmware.setStatus("1");
        firmware.setDelFlag("0");
        firmware.setCreateTime(LocalDateTime.now());
        firmware.setUpdateTime(LocalDateTime.now());

        // 设置创建人
        String userId = TenantContext.getUserId();
        if (userId != null) {
            firmware.setCreateBy(Long.valueOf(userId));
            firmware.setUpdateBy(Long.valueOf(userId));
        }

        this.save(firmware);
        log.info("固件版本创建成功: id={}, name={}, version={}", firmware.getId(), firmware.getFirmwareName(), firmware.getVersion());

        return firmware;
    }

    /**
     * 按产品分页查询固件列表（租户隔离）
     *
     * @param productId 产品ID
     * @param page      页码
     * @param size      每页大小
     * @return 分页结果
     */
    public Page<FirmwareVersion> listByProduct(Long productId, int page, int size) {
        Page<FirmwareVersion> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<FirmwareVersion> wrapper = new LambdaQueryWrapper<>();

        // 租户隔离：平台管理员可查看所有租户固件
        if (!TenantContext.isPlatformAdmin()) {
            String tenantId = TenantContext.getTenantId();
            wrapper.eq(FirmwareVersion::getTenantId, Long.valueOf(tenantId));
        }

        // 按产品过滤
        if (productId != null) {
            wrapper.eq(FirmwareVersion::getProductId, productId);
        }

        wrapper.orderByDesc(FirmwareVersion::getCreateTime);

        return this.page(pageParam, wrapper);
    }

    /**
     * 获取固件文件路径（用于下载）
     *
     * @param firmwareId 固件ID
     * @return 固件文件绝对路径
     */
    public String getFilePath(Long firmwareId) {
        FirmwareVersion firmware = getByIdWithPermissionCheck(firmwareId);
        String filePath = firmware.getFilePath();

        // 校验文件是否存在
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw BusinessException.notFound("固件文件不存在: " + filePath);
        }

        return filePath;
    }

    /**
     * 根据ID查询固件版本（带租户权限校验）
     *
     * @param id 固件版本ID
     * @return 固件版本记录
     */
    public FirmwareVersion getByIdWithPermissionCheck(Long id) {
        FirmwareVersion firmware = this.getById(id);
        if (firmware == null) {
            throw BusinessException.notFound("固件版本不存在");
        }

        // 租户权限校验：平台管理员可访问任意租户的固件
        if (!TenantContext.isPlatformAdmin()) {
            String tenantId = TenantContext.getTenantId();
            if (!firmware.getTenantId().toString().equals(tenantId)) {
                throw BusinessException.forbidden("无权访问该固件版本");
            }
        }

        return firmware;
    }

    /**
     * 计算文件哈希值
     *
     * @param filePath  文件路径
     * @param algorithm 哈希算法（MD5 / SHA-256）
     * @return 十六进制哈希字符串
     */
    private String computeHash(Path filePath, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] hashBytes = digest.digest(fileBytes);

            // 转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            throw BusinessException.internalError("计算文件哈希失败: " + e.getMessage());
        }
    }
}
