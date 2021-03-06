package uihelpers;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;


public class TVAPI extends
		AbstractPreferenceInitializer {
	
	public static final String PLUGIN_ID = "eu.jucy.helpers.ui";

	public TVAPI() {}

	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences defaults = DefaultScope.INSTANCE .getNode(PLUGIN_ID);
		
		IExtensionRegistry reg = Platform.getExtensionRegistry();
    	
		IConfigurationElement[] configElements = reg
		.getConfigurationElementsFor(TableViewerAdministrator.ExtensionpointID);

		
		for (IConfigurationElement element : configElements) {
			try {
				if ("table".equals(element.getName()) ) {
					String tableID = element.getAttribute("id");
					for (IConfigurationElement newColumns : element.getChildren("table_column")) {
						String fullID = IDForTableColumn(tableID,newColumns.getAttribute("id"),false);
						defaults.putBoolean(fullID, true);
					}

					for (IConfigurationElement newColumns : element.getChildren("table_column_decorator")) {
						String fullID = IDForTableColumn(tableID,newColumns.getAttribute("id"),true);
						defaults.putBoolean(fullID, true);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static String IDForTableColumn(String tableID,String tableColumnID,boolean decorator) {
		return tableID+ (decorator?".dec.": ".col.")+tableColumnID;
	}
	
	
	public static boolean get(String fullID) {
	//	String fullID =IDForTableColumn(tableID,columnID,decorator);
		String s = InstanceScope.INSTANCE .getNode(PLUGIN_ID).get(fullID, null);
		if (s != null) {
			return Boolean.valueOf(s);
		}
		s = ConfigurationScope.INSTANCE .getNode(PLUGIN_ID).get(fullID,null);
		if (s != null) {
			return Boolean.valueOf(s);
		}
		
    	return Boolean.valueOf(DefaultScope.INSTANCE .getNode(PLUGIN_ID).get(fullID, "true"));
	}

}
