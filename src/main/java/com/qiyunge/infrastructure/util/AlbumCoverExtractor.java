package com.qiyunge.infrastructure.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 从音频文件中提取专辑封面。
 * 支持 MP3 (ID3v2 APIC)、M4A (iTunes 封面)、FLAC (Vorbis PICTURE)。
 * 提取后缓存到本地，避免重复解析。
 */
public class AlbumCoverExtractor {

    private static final Logger LOG = Logger.getLogger(AlbumCoverExtractor.class.getName());
    private static final Path CACHE_DIR = Path.of(System.getProperty("java.io.tmpdir"), "qiyunge-covers");

    static {
        try { Files.createDirectories(CACHE_DIR); } catch (IOException ignored) {}
    }

    /**
     * 从音频文件提取封面，返回缓存的图片路径。
     * 如果没有封面，返回 null。
     */
    public static Path extractCover(Path audioPath) {
        if (audioPath == null || !Files.exists(audioPath)) return null;

        // 检查缓存
        String cacheKey = getCacheKey(audioPath);
        Path cached = CACHE_DIR.resolve(cacheKey);
        if (Files.exists(cached)) return cached;

        try {
            BufferedImage image = null;
            String name = audioPath.getFileName().toString().toLowerCase();

            if (name.endsWith(".mp3")) {
                image = extractFromMP3(audioPath);
            } else if (name.endsWith(".m4a") || name.endsWith(".mp4")) {
                image = extractFromM4A(audioPath);
            } else if (name.endsWith(".flac")) {
                image = extractFromFLAC(audioPath);
            }

            if (image != null) {
                ImageIO.write(image, "jpg", cached.toFile());
                cached.toFile().deleteOnExit();
                return cached;
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to extract cover from " + audioPath, e);
        }
        return null;
    }

    /** MP3: 查找 ID3v2 APIC 帧 */
    private static BufferedImage extractFromMP3(Path path) throws IOException {
        byte[] data = Files.readAllBytes(path);
        // 检查 ID3v2 header
        if (data.length < 10 || data[0] != 'I' || data[1] != 'D' || data[2] != '3') return null;

        int id3Size = ((data[6] & 0x7F) << 21) | ((data[7] & 0x7F) << 14)
                   | ((data[8] & 0x7F) << 7) | (data[9] & 0x7F);
        if (id3Size < 0 || id3Size > data.length - 10) return null;
        int offset = 10;

        while (offset < Math.min(data.length, id3Size + 10)) {
            if (offset + 10 > data.length) break;
            String frameId = new String(data, offset, 4);
            int frameSize = ((data[offset + 4] & 0x7F) << 24) | ((data[offset + 5] & 0x7F) << 16)
                           | ((data[offset + 6] & 0x7F) << 8) | (data[offset + 7] & 0x7F);

            if (frameSize == 0) break;

            if ("APIC".equals(frameId)) {
                // APIC frame: encoding(1) + mime(n) + null(1) + type(1) + desc(n) + null(1) + image data
                int contentStart = offset + 10;
                int encoding = data[contentStart] & 0xFF;
                int mimeEnd = indexOf(data, 0, contentStart + 1);
                int descStart = mimeEnd + 1 + 1 + 1; // skip mime null, picture type, then desc
                // Find desc null terminator
                int descEnd = indexOf(data, 0, descStart);
                int imgStart = descEnd + 1;
                int imgEnd = offset + 10 + frameSize;

                if (imgStart < imgEnd && imgEnd <= data.length) {
                    byte[] imgData = new byte[imgEnd - imgStart];
                    System.arraycopy(data, imgStart, imgData, 0, imgData.length);
                    return ImageIO.read(new ByteArrayInputStream(imgData));
                }
            }
            offset += 10 + frameSize;
        }
        return null;
    }

    /** M4A: 查找 iTunes 'covr' atom */
    private static BufferedImage extractFromM4A(Path path) throws IOException {
        byte[] data = Files.readAllBytes(path);
        return findCoverInMP4(data, 0, data.length);
    }

    private static BufferedImage findCoverInMP4(byte[] data, int start, int end) {
        int offset = start;
        while (offset + 8 <= end) {
            int size = ((data[offset] & 0xFF) << 24) | ((data[offset + 1] & 0xFF) << 16)
                     | ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
            String type = new String(data, offset + 4, 4);

            if (size <= 0 || offset + size > end) break;

            if ("moov".equals(type) || "trak".equals(type) || "mdia".equals(type) || "meta".equals(type)) {
                // Container atom, recurse
                int childStart = "meta".equals(type) ? offset + 12 : offset + 8;
                BufferedImage result = findCoverInMP4(data, childStart, offset + size);
                if (result != null) return result;
            } else if ("covr".equals(type)) {
                // Cover art data
                int dataStart = offset + 8;
                byte[] imgData = new byte[size - 8];
                System.arraycopy(data, dataStart, imgData, 0, imgData.length);
                try {
                    return ImageIO.read(new ByteArrayInputStream(imgData));
                } catch (IOException e) {
                    return null;
                }
            }
            offset += size;
        }
        return null;
    }

    /** FLAC: 查找 PICTURE metadata block */
    private static BufferedImage extractFromFLAC(Path path) throws IOException {
        byte[] data = Files.readAllBytes(path);
        // FLAC magic: "fLaC"
        if (data.length < 4 || data[0] != 'f' || data[1] != 'L' || data[2] != 'a' || data[3] != 'C') return null;

        int offset = 4;
        while (offset + 4 <= data.length) {
            int blockHeader = (data[offset] & 0xFF);
            boolean isLast = (blockHeader & 0x80) != 0;
            int blockType = blockHeader & 0x7F;
            int blockSize = ((data[offset + 1] & 0xFF) << 16) | ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);

            if (blockType == 6) { // PICTURE block
                // PICTURE format: type(4) + mime_len(4) + mime + desc_len(4) + desc + width(4) + height(4) + ... + image_data
                int picOffset = offset + 4;
                if (picOffset + 4 > data.length) break;
                // Skip picture type (4 bytes)
                picOffset += 4;
                // MIME type
                int mimeLen = ((data[picOffset] & 0xFF) << 24) | ((data[picOffset + 1] & 0xFF) << 16)
                           | ((data[picOffset + 2] & 0xFF) << 8) | (data[picOffset + 3] & 0xFF);
                picOffset += 4 + mimeLen;
                // Description
                if (picOffset + 4 > data.length) break;
                int descLen = ((data[picOffset] & 0xFF) << 24) | ((data[picOffset + 1] & 0xFF) << 16)
                           | ((data[picOffset + 2] & 0xFF) << 8) | (data[picOffset + 3] & 0xFF);
                picOffset += 4 + descLen;
                // Skip width(4) + height(4) + colorDepth(4) + colorCount(4)
                picOffset += 16;
                // Image data
                if (picOffset < offset + 4 + blockSize) {
                    int imgLen = offset + 4 + blockSize - picOffset;
                    byte[] imgData = new byte[imgLen];
                    System.arraycopy(data, picOffset, imgData, 0, imgLen);
                    return ImageIO.read(new ByteArrayInputStream(imgData));
                }
            }
            offset += 4 + blockSize;
            if (isLast) break;
        }
        return null;
    }

    private static int indexOf(byte[] data, int target, int from) {
        for (int i = from; i < data.length; i++) {
            if (data[i] == target) return i;
        }
        return data.length;
    }

    private static String getCacheKey(Path audioPath) {
        long size = 0;
        long modified = 0;
        try {
            size = Files.size(audioPath);
            modified = Files.getLastModifiedTime(audioPath).toMillis();
        } catch (IOException ignored) {}
        return audioPath.getFileName().toString().hashCode() + "-" + size + "-" + modified + ".jpg";
    }
}
