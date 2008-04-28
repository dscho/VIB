package org.imagearchive.lsm.toolbox;

import ij.IJ;

import java.util.Vector;

import org.imagearchive.lsm.toolbox.gui.ControlPanelFrame;
import org.imagearchive.lsm.toolbox.gui.InfoFrame;
import org.imagearchive.lsm.toolbox.info.CZ_LSMInfo;
import org.imagearchive.lsm.toolbox.info.ImageDirectory;
import org.imagearchive.lsm.toolbox.info.LsmFileInfo;
import org.imagearchive.lsm.toolbox.info.scaninfo.Recording;

public class MasterModel {

	private LsmFileInfo lsm;

	private ServiceMediator serviceMediator;

	private ControlPanelFrame controlPanelFrame;

	private InfoFrame infoFrame;

	private Vector masterModelListeners = new Vector();

    public static final String VERSION = "4.0c";

    public static boolean debugMode = false;
    
    public static char micro = '\u00b5';
    
    public static String micrometer = micro+"m";
    
    private CZ_LSMInfo cz;

	private ImageDirectory firstImageDir;
	
	public String[] supportedBatchTypes = { "Tiff", "8-bit Tiff", "Jpeg","Zip", "Raw" };
	
	public MasterModel() {
		initializeModel();
		registerServices();
	}

	public ServiceMediator getServiceMediator() {
		return serviceMediator;
	}

	protected void setServiceMediator(ServiceMediator serviceMediator) {
		this.serviceMediator = serviceMediator;
	}

	public void initializeModel() {
		serviceMediator = new ServiceMediator();
		lsm = new LsmFileInfo(this);
	}

	public LsmFileInfo getLsmFileInfo() {
		return lsm;
	}

	public void setLSMFI(LsmFileInfo lsmfi) {
		lsm = lsmfi;
		if (lsm != null) {
			firstImageDir = (ImageDirectory) lsm.imageDirectories.get(0);
			if (firstImageDir != null)
				setCz(firstImageDir.TIF_CZ_LSMINFO);
		}
		fireLSMFileInfoChanged();
	}

	public void setControlPanelFrame(ControlPanelFrame controlPanelFrame) {
		this.controlPanelFrame = controlPanelFrame;
		serviceMediator.registerControlPanelFrame(controlPanelFrame);
	}

	public void setInfoFrame(InfoFrame infoFrame) {
		this.infoFrame = infoFrame;
		serviceMediator.registerInfoFrame(infoFrame);
	}

	private void registerServices() {
		serviceMediator.registerMasterModel(this);
		serviceMediator.registerControlPanelFrame(controlPanelFrame);
		serviceMediator.registerInfoFrame(infoFrame);
	}

	public void fireLSMFileInfoChanged() {
		Vector tl;
		synchronized (this) {
			tl = (Vector) masterModelListeners.clone();
		}
		int size = tl.size();
		if (size == 0) {
			return;
		}
		MasterModelEvent event = new MasterModelEvent(this);
		for (int i = 0; i < size; ++i) {
			MasterModelListener listener = (MasterModelListener) tl
					.elementAt(i);
			listener.LSMFileInfoChanged(event);
		}
	}

	public synchronized void addMasterModelListener(MasterModelListener l) {
		if (masterModelListeners.contains(l)) {
			return;
		}
		masterModelListeners.addElement(l);
	}

	public synchronized void removeMasterModelListener(MasterModelListener l) {
		masterModelListeners.removeElement(l);
	}

	public synchronized void removeAllListeners() {
		masterModelListeners.clear();
	}

	/** *************************************************************************** */
	public String getVersion() {
		return VERSION;
	}

	public String[] getInfo() {
		if (lsm == null) return null;
		String[] infos = new String[19];
		String stacksize = IJ.d2s(getCz().DimensionZ, 0);
		String width = IJ.d2s(getLsmFileInfo().width, 0);
		String height = IJ.d2s(getLsmFileInfo().height, 0);
		String channels = IJ.d2s(getCz().DimensionChannels, 0);
		String scantype = "";

		switch ((int) getCz().ScanType) {
		case 0:
			scantype = "Normal X-Y-Z scan";
			break;
		case 1:
			scantype = "Z scan";
			break;
		case 2:
			scantype = "Line scan";
			break;
		case 3:
			scantype = "Time series X-Y";
			break;
		case 4:
			scantype = "Time series X-Z";
			break;
		case 5:
			scantype = "Time series - Means of ROIs";
			break;
		case 6:
			scantype = "Time series X-Y-Z";
			break;
		default:
			scantype = "UNKNOWN !";
			break;
		}
		Recording r = (Recording) getCz().scanInfo.recordings.get(0);
		String objective = (String) r.records.get("ENTRY_OBJECTIVE");
		String user = (String) r.records.get("USER");
		double zoomx = ((Double) r.records.get("ZOOM_X")).doubleValue();
		double zoomy = ((Double) r.records.get("ZOOM_Y")).doubleValue();

		double zoomz = ((Double) r.records.get("ZOOM_Z")).doubleValue();

		double planeSpacing = ((Double) r.records.get("PLANE SPACING"))
				.doubleValue();

		String voxelsize_x = IJ.d2s(getCz().VoxelSizeX * 1000000, 2) + " "+micrometer;
		String voxelsize_y = IJ.d2s(getCz().VoxelSizeY * 1000000, 2) + " "+micrometer;
		String voxelsize_z = IJ.d2s(getCz().VoxelSizeZ * 1000000, 2) + " "+micrometer;
		String timestacksize = IJ.d2s(getCz().DimensionTime, 0);
		String plane_spacing = IJ.d2s(planeSpacing, 2) + " "+micrometer;;
		String plane_width = IJ.d2s(getCz().DimensionX * getCz().VoxelSizeX, 2)
				+ " "+micrometer;
		String plane_height = IJ
				.d2s(getCz().DimensionY * getCz().VoxelSizeY, 2)
				+ " "+micrometer;
		String volume_depth = IJ
				.d2s(getCz().DimensionZ * getCz().VoxelSizeZ, 2)
				+ " "+micrometer;

		infos[0] = getLsmFileInfo().fileName;
		infos[1] = user;
		infos[2] = width;
		infos[3] = height;
		infos[4] = channels;
		infos[5] = stacksize;
		infos[6] = timestacksize;
		infos[7] = scantype;
		infos[8] = voxelsize_x;
		infos[9] = voxelsize_y;
		infos[10] = voxelsize_z;
		infos[11] = objective;
		infos[12] = IJ.d2s(zoomx, 2);
		infos[13] = IJ.d2s(zoomy, 2);
		infos[14] = IJ.d2s(zoomz, 2);
		infos[15] = plane_width;
		infos[16] = plane_height;
		infos[17] = volume_depth;
		infos[18] = plane_spacing;
		return infos;
	}

	public CZ_LSMInfo getCz() {
		return cz;
	}

	public void setCz(CZ_LSMInfo cz) { // conveniency method
		this.cz = cz;
	}
}
