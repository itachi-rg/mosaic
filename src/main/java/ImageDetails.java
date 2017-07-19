import org.bytedeco.javacpp.opencv_core;

import java.io.File;

/**
 * Created by rg on 26-Nov-16.
 */
public class ImageDetails {
    File imageFile;
    int resizedWidth;
    int resizedHeight;
    int resizedDepth;
    int resizedChannels;
    opencv_core.IplImage image;
    opencv_core.Mat img;

    double[] pixelAverage = new double[3];


    public opencv_core.IplImage getImage() {
        return image;
    }

    public void setImage(opencv_core.IplImage image) {
        this.image = image;
    }

    public opencv_core.Mat getImg() {
        return img;
    }

    public void setImg(opencv_core.Mat img) {
        this.img = img;
    }

    public ImageDetails(File imageFile) {
        this.imageFile = imageFile;
    }

    public File getImageFile() {
        return imageFile;
    }

    public void setImageFile(File imageFile) {
        this.imageFile = imageFile;
    }

    public int getResizedWidth() {
        return resizedWidth;
    }

    public void setResizedWidth(int resizedWidth) {
        this.resizedWidth = resizedWidth;
    }

    public int getResizedHeight() {
        return resizedHeight;
    }

    public void setResizedHeight(int resizedHeight) {
        this.resizedHeight = resizedHeight;
    }

    public int getResizedDepth() {
        return resizedDepth;
    }

    public void setResizedDepth(int resizedDepth) {
        this.resizedDepth = resizedDepth;
    }

    public int getResizedChannels() {
        return resizedChannels;
    }

    public void setResizedChannels(int resizedChannels) {
        this.resizedChannels = resizedChannels;
    }

    public double[] getPixelAverage() {
        return pixelAverage;
    }

    public void setPixelAverage(double[] pixelAverage) {
        this.pixelAverage = pixelAverage;
    }
}
