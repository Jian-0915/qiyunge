package com.qiyunge.infrastructure.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HexFormat;

/**
 * 通过文件头魔数识别音频文件的真实格式，不依赖文件后缀。
 */
public class AudioFormatDetector {

    public record AudioFormat(String format, String codec, String label) {}

    /**
     * 检测音频文件的真实格式。
     * @return AudioFormat(format, codec, label)，无法识别时返回 null
     */
    public static AudioFormat detect(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[12];
            int n = fis.read(header);
            if (n < 4) return null;

            String hex = HexFormat.of().formatHex(header, 0, n).toUpperCase();

            // ID3v2 tag → MP3
            if (hex.startsWith("494433")) {
                return new AudioFormat("mp3", "MP3", "MP3");
            }

            // MPEG Audio Layer 1/2/3 sync: 0xFF followed by 0xE0 mask
            if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xE0) == 0xE0) {
                int layerBits = (header[1] >> 1) & 0x03;
                String layer = switch (layerBits) {
                    case 1 -> "Layer III";
                    case 2 -> "Layer II";
                    case 3 -> "Layer I";
                    default -> "MPEG";
                };
                return new AudioFormat("mp3", layer, "MP3");
            }

            // fLaC → FLAC
            if (hex.startsWith("664C6143")) {
                return new AudioFormat("flac", "FLAC", "FLAC");
            }

            // OggS → OGG (Vorbis or Opus)
            if (hex.startsWith("4F676753")) {
                return new AudioFormat("ogg", "OGG", "OGG");
            }

            // RIFF....WAVE → WAV
            if (hex.startsWith("52494646") && n >= 12) {
                String wave = new String(header, 8, 4);
                if ("WAVE".equals(wave)) {
                    return new AudioFormat("wav", "PCM", "WAV");
                }
                // RIFF with other format
                return new AudioFormat("wav", "RIFF", "WAV");
            }

            // M4A / AAC: ftyp box at offset 4
            if (n >= 12 && hex.startsWith("000000") || hex.startsWith("0000001")) {
                // Check for ftyp at offset 4
                String ftyp = new String(header, 4, 4);
                if ("ftyp".equals(ftyp)) {
                    String brand = new String(header, 8, 4);
                    // M4A brands: M4A, isom, iso2, mp41, mp42, avc1, etc.
                    if ("M4A ".equals(brand) || "m4a ".equals(brand)) {
                        return new AudioFormat("m4a", "AAC", "M4A");
                    }
                    // Generic MPEG-4 container → likely AAC audio
                    return new AudioFormat("m4a", "AAC", "M4A");
                }
            }

            // Also check ftyp at offset 0 with size prefix
            if (n >= 8) {
                String ftypCheck = new String(header, 4, 4);
                if ("ftyp".equals(ftypCheck)) {
                    return new AudioFormat("m4a", "AAC", "M4A");
                }
            }

            // WMA / ASF: 30 26 B2 75 8E 66 CF 11
            if (hex.startsWith("3026B2758E66CF11")) {
                return new AudioFormat("wma", "WMA", "WMA");
            }

            // AMR: #!AMR
            if (hex.startsWith("2321414D52")) {
                return new AudioFormat("amr", "AMR", "AMR");
            }

            // Unknown header, fallback to extension
            return null;

        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 从文件后缀推断格式（作为 fallback）。
     */
    public static AudioFormat fromExtension(String fileName) {
        if (fileName == null) return null;
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) return null;
        String ext = fileName.substring(dot + 1).toLowerCase();
        return switch (ext) {
            case "mp3" -> new AudioFormat("mp3", "MP3", "MP3");
            case "m4a", "mp4" -> new AudioFormat("m4a", "AAC", "M4A");
            case "flac" -> new AudioFormat("flac", "FLAC", "FLAC");
            case "wav" -> new AudioFormat("wav", "PCM", "WAV");
            case "ogg", "oga" -> new AudioFormat("ogg", "OGG", "OGG");
            case "wma" -> new AudioFormat("wma", "WMA", "WMA");
            case "aac" -> new AudioFormat("aac", "AAC", "AAC");
            default -> null;
        };
    }
}
