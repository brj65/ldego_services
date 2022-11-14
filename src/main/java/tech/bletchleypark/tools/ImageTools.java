package tech.bletchleypark.tools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;

import javax.imageio.ImageIO;

public class ImageTools {
    public static Path base64ToImage(String stringImage, String fileName) throws IOException {
        if (!stringImage.startsWith("data:image") || !stringImage.contains("base64"))
            return null;
        byte[] imageBytes = javax.xml.bind.DatatypeConverter
                .parseBase64Binary(stringImage.substring(stringImage.indexOf(",") + 1));
        BufferedImage bi = ImageIO.read(new ByteArrayInputStream(imageBytes));
        Path outputFile = Files.createTempFile(fileName, ".png");
        ImageIO.write(bi, "png", outputFile.toFile());
        return outputFile;
    }


    public static float compareImage(Path fileA, Path fileB) {

        float percentage = 0;
        try {
            // take buffer data from both image files //
            BufferedImage biA = ImageIO.read(fileA.toFile());
            DataBuffer dbA = biA.getData().getDataBuffer();
            int sizeA = dbA.getSize();
            BufferedImage biB = ImageIO.read(fileB.toFile());
            DataBuffer dbB = biB.getData().getDataBuffer();
          //  int sizeB = dbB.getSize();
            int count = 0;
            // compare data-buffer objects //
       //     if (sizeA == sizeB) {
    
                for (int i = 0; i < sizeA; i++) {
    
                    if (dbA.getElem(i) == dbB.getElem(i)) {
                        count = count + 1;
                    }
    
                }
                percentage = (count * 100) / sizeA;
         //   } else {
    //            System.out.println("Both the images are not of same size");
         //   }
    
        } catch (Exception e) {
            System.out.println("Failed to compare image files ...");
        }
        return percentage;
    }

}
