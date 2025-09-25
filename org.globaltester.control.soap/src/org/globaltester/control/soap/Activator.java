package org.globaltester.control.soap;

import java.util.Hashtable;

import javax.xml.ws.Endpoint;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.globaltester.control.RemoteControlHandler;
import org.globaltester.control.soap.preferences.PreferenceConstants;
import org.globaltester.service.GtService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;


/**
 * This manages the services needed to supply the SOAP {@link Endpoint}
 *
 * @author mboonk
 *
 */
public class Activator extends AbstractUIPlugin
{
	private static BundleContext context;
	private static Activator plugin;

	public static BundleContext getContext()
	{
		return context;
	}

	public static Activator getDefault()
	{
		return plugin;
	}

	private SoapControlEndpointManager endpointManager;
	private ServiceRegistration<GtService> gtServiceRegistration;
	private ServiceRegistration<RemoteControlHandler> preferenceManagerRegistration;
	private ServiceRegistration<RemoteControlHandler> propertyManagerRegistration;
	private ServiceRegistration<RemoteControlHandler> workspaceManagerRegistration;


	@Override
	public void start(BundleContext context) throws Exception
	{
		// BasicLogger.log("START Activator Control SOAP", LogLevel.TRACE);
		Activator.context = context;
		Activator.plugin = this;

			// register endpointManager as GtService
			endpointManager = new SoapControlEndpointManager();
			gtServiceRegistration = context.registerService(GtService.class, endpointManager, new Hashtable<String, String>());

			registerServices();

			// handle autostart
			boolean autostart = getPreferenceStore().getBoolean(PreferenceConstants.P_SOAP_AUTOSTART);
			if (autostart) {
				endpointManager.start();
			}

		// BasicLogger.log("END Activator Control SOAP", LogLevel.TRACE);
	}


	@Override
	public void stop(BundleContext bundleContext) throws Exception
	{
		if (endpointManager != null && endpointManager.isRunning()) {
			endpointManager.stop();
			endpointManager = null;
		}
		if (gtServiceRegistration != null) {
			gtServiceRegistration.unregister();
			gtServiceRegistration = null;
		}
		unregisterServices();
		context = null;
	}

	public void registerServices()
	{
		unregisterServices();
		preferenceManagerRegistration = context.registerService(RemoteControlHandler.class, new PreferenceManagerSoap(), new Hashtable<String, String>());
		propertyManagerRegistration = context.registerService(RemoteControlHandler.class, new PropertyManagerSoap(), new Hashtable<String, String>());
		workspaceManagerRegistration = context.registerService(RemoteControlHandler.class, new WorkspaceManagerSoap(), new Hashtable<String, String>());
	}

	public void unregisterServices()
	{
		if (preferenceManagerRegistration != null) {
			preferenceManagerRegistration.unregister();
			preferenceManagerRegistration = null;
		}
		if (propertyManagerRegistration != null) {
			propertyManagerRegistration.unregister();
			propertyManagerRegistration = null;
		}
		if (workspaceManagerRegistration != null) {
			workspaceManagerRegistration.unregister();
			workspaceManagerRegistration = null;
		}
	}
}
