import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Random;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
 
public class PixelGenerator {
    private static String OUTPUT_PREFIX;
    private static int MAX_NUM_PARTS;
    private static int MAX_NUM_POINTS_PER_PART;
    private static final Random RANDOM = new Random();
    private static final double RANGE_MIN = 0.0;
    private static final double RANGE_MAX = 10000;
    
    public static void  main(String[] args) {
        String inputFile = args[0];
        String outputFile = args[1];

        long start = System.currentTimeMillis();
        BufferedImage source = null;

        try {
            source = ImageIO.read(new File(inputFile));
            int width = source.getWidth();
            int height = source.getHeight();
            width = (int)Math.round((double)width/2);
            height = (int)Math.round((double)height/2);
            int [][] pixelValue = new int[height][width];
            int [][] rgb = new int[height][width];
            PrintWriter writer = new PrintWriter(new FileWriter(outputFile, true));
            writer.print(height + " " + width + " ");
            for(int i = 0; i < pixelValue.length; i++) {
                for(int j = 0; j < pixelValue[i].length; j++) {
                    pixelValue[i][j] = source.getRGB(j, i);
                    int[] RGB = getRGBFromPixel(pixelValue[i][j]);
                    writer.print(RGB[1]+ " " + RGB[2] + " " + RGB[3] + " ");
                }
            }
            writer.println("");
            writer.close();
        } catch(IOException e) {
          e.printStackTrace();
        }
        //BufferedImage imageDest = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        System.out.println(((System.currentTimeMillis()-start)/1000f)+"s");
    }
    private static int[] getRGBFromPixel(int pixelvalue) {
        int[] RGB = new int[4];
        int alpha = (pixelvalue >> 24) & 0xff;
        int red = (pixelvalue >> 16) & 0xff;
        int green = (pixelvalue >> 8 ) & 0xff;
        int blue = pixelvalue & 0xff;

        RGB[0] = alpha;
        RGB[1] = red;
        RGB[2] = green;
        RGB[3] = blue;

        return RGB;
    }  
    private static int returnToRGB(int R, int G, int B) {
        int RGB = 0x00000000;
        int alpha = (0xff << 24);

        /* process Red value */
        int Red = (R & 0xff) << 16;

        /* process Green value */
        int Green = (G & 0xff) << 8;

        /* process Blue value */
        int Blue = (B & 0xff);

        RGB = alpha | Red | Green | Blue;

        return RGB;
    }
}
