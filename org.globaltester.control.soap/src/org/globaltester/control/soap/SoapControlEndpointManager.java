package org.globaltester.control.soap;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.ws.Endpoint;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.statushandlers.StatusManager;
import org.globaltester.base.PreferenceHelper;
import org.globaltester.base.utils.Utils;
import org.globaltester.control.RemoteControlHandler;
import org.globaltester.control.soap.preferences.PreferenceConstants;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;
import org.globaltester.service.AbstractGtService;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * This manages the services needed to supply the SOAP {@link Endpoint}
 *
 * @author mboonk
 *
 */
public class SoapControlEndpointManager extends AbstractGtService
{

	private static final int MAX_TRIES = 10;
	private Endpoint controlEndpoint;
	private List<Endpoint> additionalEndpoints = new LinkedList<>();
	private SoapServiceProviderData data = new SoapServiceProviderData();
	private ServiceTracker<RemoteControlHandler, RemoteControlHandler> handlerTracker;

	private String host;
	private int port;

	private String endpointInfix = "globaltester";

	public SoapControlEndpointManager()
	{

	}

	public SoapControlEndpointManager(String endpointInfix)
	{
		this.endpointInfix = endpointInfix;
	}

	@Override
	public String getName()
	{
		return "SOAP Control";
	}

	@Override
	public boolean isRunning()
	{
		return controlEndpoint != null;
	}

	@Override
	public void start()
	{
		port = Activator.getDefault().getPreferenceStore().getInt(PreferenceConstants.P_SOAP_PORT);
		start(port);
	}

	public void start(int portToUse)
	{
		if (Activator.getDefault() == null || Activator.getDefault().getPreferenceStore() == null) {
			stop();
			return;
		}
		host = Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.P_SOAP_HOST);
		port = portToUse;

		String portProperty = System.getProperty("org.globaltester.control.soap.port");
		try {
			port = Integer.parseInt(portProperty);
		}
		catch (NumberFormatException e) {
			// ignore intentionally, don't use unparseable values from properties
		}

		try {
			publishEndpoint();
		}
		catch (RuntimeException e) {
			Display.getDefault()
					.asyncExec(() -> MessageDialog.openWarning(null, "Warning",
							"Socket for SOAP already in use by another service or unreachable!\n" + "Tried host " + host + " with port " + port
									+ ". Please change them in your preferences and restart the service.\n" + "Alternatively, deactivate SOAP in the preferences to avoid this warning in the future.\n"
									+ "This is also a common issue if multiple instances are started."));

			BasicLogger.logException(getClass(), "Error during soap end point publishing", e);

			logSocketError();
		}

		// This will be used to keep track of handlers as they are un/registering
		ServiceTrackerCustomizer<RemoteControlHandler, RemoteControlHandler> handlerCustomizer = new ServiceTrackerCustomizer<RemoteControlHandler, RemoteControlHandler>() {

			@Override
			public void removedService(ServiceReference<RemoteControlHandler> reference, RemoteControlHandler service)
			{
				data.removeHandler(service);

				Endpoint toRemove = null;
				for (Endpoint endpoint : additionalEndpoints) {
					Object implementor = endpoint.getImplementor();
					if (implementor instanceof AbstractProxy<?> && service == ((AbstractProxy<?>) implementor).getHandler()) {
						endpoint.stop();
						toRemove = endpoint;
						break;
					}
				}
				additionalEndpoints.remove(toRemove);
			}

			@Override
			public void modifiedService(ServiceReference<RemoteControlHandler> reference, RemoteControlHandler service)
			{
				// nothing to do
			}

			@Override
			public RemoteControlHandler addingService(ServiceReference<RemoteControlHandler> reference)
			{
				RemoteControlHandler handlerService = Activator.getContext().getService(reference);
				handleRemoteControl(handlerService);
				return handlerService;
			}
		};

		handlerTracker = new ServiceTracker<>(Activator.getContext(), RemoteControlHandler.class, handlerCustomizer);
		handlerTracker.open();


		// notify GtServiceListeners of new status
		this.notifyStatusChange(true);
	}

	private void publishEndpoint()
	{
		if (port == 0) {
			for (int i = 0; i < MAX_TRIES; i++) {
				try {
					port = Utils.getAvailablePort();
					String endpoint = "http://" + host + ":" + port + "/" + endpointInfix + "/RemoteControl";
					BasicLogger.log(getClass(), "Publishing endpoint '" + endpoint + "'.", LogLevel.DEBUG);
					controlEndpoint = Endpoint.publish(endpoint, new RemoteControlSoap(data));
					PreferenceHelper.setPreferenceValue(Activator.getContext().getBundle().getSymbolicName(), PreferenceConstants.P_SOAP_PORT, port + "");
					break;
				}
				catch (RuntimeException | IOException e) {
					// do nothing and try again
				}
			}
		}
		else {
			String endpoint = "http://" + host + ":" + port + "/" + endpointInfix + "/RemoteControl";
			BasicLogger.log(getClass(), "Publishing endpoint '" + endpoint + "'.", LogLevel.DEBUG);
			controlEndpoint = Endpoint.publish(endpoint, new RemoteControlSoap(data));
		}
	}

	@Override
	public void stop()
	{
		if (handlerTracker != null) {
			handlerTracker.close();
			handlerTracker = null;
		}

		if (controlEndpoint != null) {
			controlEndpoint.stop();
			controlEndpoint = null;
		}

		for (Endpoint endpoint : additionalEndpoints) {
			endpoint.stop();
		}
		additionalEndpoints.clear();

		// notify GtServiceListeners of new status
		this.notifyStatusChange(false);
	}

	private void handleRemoteControl(RemoteControlHandler handlerService)
	{
		data.addHandler(handlerService);
		Endpoint newEndpoint;
		try {
			JaxWsSoapAdapter handlerAdapter = handlerService.getAdapter(JaxWsSoapAdapter.class);
			if (handlerAdapter == null) {
				return;
			}

			newEndpoint = Endpoint.publish("http://" + host + ":" + port + "/" + endpointInfix + "/" + handlerService.getIdentifier(), handlerAdapter);
			additionalEndpoints.add(newEndpoint);
		}
		catch (Exception e) {
			logSocketError();
		}
	}

	/**
	 * Adds an error (about Socket for SOAP already in use) to the eclipse
	 * logging view
	 */
	private void logSocketError()
	{
		Display.getDefault().asyncExec(() -> {
			IStatus status = new Status(IStatus.ERROR, getClass().getPackage().getName(), "Socket for SOAP is already in use by another Service");
			StatusManager.getManager().handle(status, StatusManager.LOG);
		});
	}
}
