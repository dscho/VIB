
import ij.ImagePlus;

/**
 * Wrapper clas for an ImagePlus, stored vairous objects in the properties of the Image plus
 * OBjects used for segmentating
 * User: Tom Larkworthy
 * Date: 21-Jun-2006
 * Time: 00:08:57
 */
public class SegmentatorModel {
    final ImagePlus data;

    public static final String LABEL_IMAGE_PLUS = "SegmentatorModel.labels";
    public static final String LABEL_CANVAS = "SegmentatorModel.labels.canvas";
    public static final String CURRENT_MATERIAL = "SegmentatorModel.currentMaterial";

    public SegmentatorModel(ImagePlus data) {
        this.data = data;
    }

    public ImagePlus getLabelImagePlus(){
        return (ImagePlus) data.getProperty(LABEL_IMAGE_PLUS);
    }
    public void setLabelImagePlus(ImagePlus ip){
        data.setProperty(LABEL_IMAGE_PLUS, ip);
    }

    public SegmentationViewer_.SegViewerCanvas getLabelCanvas(){
        return (SegmentationViewer_.SegViewerCanvas) data.getProperty(LABEL_CANVAS);
    }
    public void setLabelCanvas(SegmentationViewer_.SegViewerCanvas canvas){
        data.setProperty(LABEL_CANVAS, canvas);
    }

    public AmiraParameters getMaterialParams() {
        return new AmiraParameters(getLabelImagePlus());
    }

    public void setCurrentMaterial(AmiraParameters.Material currentMaterial) {
        data.setProperty(CURRENT_MATERIAL, currentMaterial);
    }

    public AmiraParameters.Material getCurrentMaterial() {
        return (AmiraParameters.Material) data.getProperty(CURRENT_MATERIAL);
    }

    public void updateSlice(int z) {
        getLabelCanvas().updateSlice(z);
        data.updateAndDraw();
    }
}
