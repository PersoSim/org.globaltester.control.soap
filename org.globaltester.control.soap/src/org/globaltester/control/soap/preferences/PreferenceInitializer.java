package org.globaltester.control.soap.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.globaltester.control.soap.Activator;


/**
 * This class stores the properties of the plugin
 * Class used to initialize default preference values
 *
 * @version Release 0.5.0
 * @author Jacob Goeke
 *
 */

public class PreferenceInitializer extends AbstractPreferenceInitializer
{

	public static final String SOAP_HOST_DEFAULT = "localhost";
	public static final int SOAP_PORT_DEFAULT = 8888;
	public static final boolean SOAP_AUTOSTART_DEFAULT = false;

	/*
	 * Use this to store plugin preferences
	 * For meaning of each preference look at PreferenceConstants.java
	 */
	@Override
	public void initializeDefaultPreferences()
	{
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		store.setDefault(PreferenceConstants.P_SOAP_HOST, SOAP_HOST_DEFAULT);
		store.setDefault(PreferenceConstants.P_SOAP_PORT, SOAP_PORT_DEFAULT);
		store.setDefault(PreferenceConstants.P_SOAP_AUTOSTART, SOAP_AUTOSTART_DEFAULT);
	}

}
