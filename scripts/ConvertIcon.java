import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.*;
import java.nio.file.*;

public class ConvertIcon {
    public static void main(String[] args) throws Exception {
        Path source = Paths.get("d:/QiyunGe/应用图标.jpg");
        Path pngTarget = Paths.get("d:/QiyunGe/src/main/resources/images/icon.png");
        Path icoTarget = Paths.get("d:/QiyunGe/src/main/resources/images/icon.ico");

        BufferedImage original = ImageIO.read(source.toFile());
        if (original == null) {
            System.err.println("Cannot read source image: " + source);
            System.exit(1);
        }
        System.out.println("Source: " + original.getWidth() + "x" + original.getHeight());

        int[] sizes = {256, 128, 64, 48, 32, 16};
        BufferedImage[] resized = new BufferedImage[sizes.length];

        for (int i = 0; i < sizes.length; i++) {
            int s = sizes[i];
            BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(original, 0, 0, s, s, null);
            g.dispose();
            resized[i] = img;
        }

        ImageIO.write(resized[0], "PNG", pngTarget.toFile());
        System.out.println("PNG saved: " + pngTarget);

        writeIcoBmp(resized, icoTarget);
        System.out.println("ICO saved: " + icoTarget);
    }

    private static void writeIcoBmp(BufferedImage[] images, Path target) throws IOException {
        int count = images.length;
        byte[][] bmpData = new byte[count][];
        int totalSize = 6 + 16 * count;

        for (int i = 0; i < count; i++) {
            bmpData[i] = toBmpWithMask(images[i]);
            totalSize += bmpData[i].length;
        }

        try (OutputStream os = Files.newOutputStream(target);
             DataOutputStream dos = new DataOutputStream(os)) {

            dos.writeShort(0);
            dos.writeShort(1);
            dos.writeShort(count);

            int offset = 6 + 16 * count;
            for (int i = 0; i < count; i++) {
                int w = images[i].getWidth();
                int h = images[i].getHeight();
                dos.writeByte(w >= 256 ? 0 : w);
                dos.writeByte(h >= 256 ? 0 : h);
                dos.writeByte(0);
                dos.writeByte(0);
                dos.writeShort(1);
                dos.writeShort(32);
                dos.writeInt(bmpData[i].length);
                dos.writeInt(offset);
                offset += bmpData[i].length;
            }

            for (byte[] data : bmpData) {
                dos.write(data);
            }
        }
    }

    private static byte[] toBmpWithMask(BufferedImage img) throws IOException {
        int w = img.getWidth();
        int h = img.getHeight();

        int rowSize = ((w * 4 + 3) / 4) * 4;
        int xorSize = rowSize * h;
        int andRowSize = ((w + 31) / 32) * 4;
        int andSize = andRowSize * h;
        int pixelDataSize = xorSize + andSize;
        int fileSize = 40 + pixelDataSize;

        ByteBuffer bb = ByteBuffer.allocate(fileSize);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        bb.putInt(40);
        bb.putInt(w);
        bb.putInt(h * 2);
        bb.putShort((short) 1);
        bb.putShort((short) 32);
        bb.putInt(0);
        bb.putInt(pixelDataSize);
        bb.putInt(2835);
        bb.putInt(2835);
        bb.putInt(0);
        bb.putInt(0);

        int[] pixels = new int[w * h];
        img.getRGB(0, 0, w, h, pixels, 0, w);

        for (int y = h - 1; y >= 0; y--) {
            int rowStart = y * w;
            for (int x = 0; x < w; x++) {
                int p = pixels[rowStart + x];
                bb.putInt(p);
            }
            int pad = rowSize - w * 4;
            for (int k = 0; k < pad; k++) bb.put((byte) 0);
        }

        for (int y = h - 1; y >= 0; y--) {
            int rowStart = y * w;
            int bitIndex = 0;
            byte currentByte = 0;
            for (int x = 0; x < w; x++) {
                int p = pixels[rowStart + x];
                int alpha = (p >>> 24) & 0xFF;
                if (alpha < 128) {
                    currentByte |= (byte) (1 << (7 - bitIndex));
                }
                bitIndex++;
                if (bitIndex == 8) {
                    bb.put(currentByte);
                    currentByte = 0;
                    bitIndex = 0;
                }
            }
            if (bitIndex > 0) bb.put(currentByte);
            int pad = andRowSize - ((w + 7) / 8);
            for (int k = 0; k < pad; k++) bb.put((byte) 0);
        }

        return bb.array();
    }
}
