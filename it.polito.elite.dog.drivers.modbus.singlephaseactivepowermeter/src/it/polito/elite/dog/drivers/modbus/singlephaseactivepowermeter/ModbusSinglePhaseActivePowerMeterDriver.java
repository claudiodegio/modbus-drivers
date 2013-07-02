/**
 * 
 */
package it.polito.elite.dog.drivers.modbus.singlephaseactivepowermeter;

import it.polito.elite.dog.drivers.modbus.gateway.ModbusGatewayDriver;
import it.polito.elite.dog.drivers.modbus.network.info.ModbusInfo;
import it.polito.elite.dog.drivers.modbus.network.interfaces.ModbusNetwork;
import it.polito.elite.domotics.dog2.doglibrary.DogDeviceCostants;
import it.polito.elite.domotics.dog2.doglibrary.devicecategory.ControllableDevice;
import it.polito.elite.domotics.dog2.doglibrary.util.DogLogInstance;
import it.polito.elite.domotics.model.devicecategory.SinglePhaseActivePowerMeter;

import java.util.Hashtable;
import java.util.Vector;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.device.Device;
import org.osgi.service.device.Driver;
import org.osgi.service.log.LogService;

/**
 * @author bonino
 * 
 */
public class ModbusSinglePhaseActivePowerMeterDriver implements Driver
{
	// The OSGi framework context
	protected BundleContext context;
	
	// System logger
	LogService logger;
	
	// the log identifier, unique for the class
	public static String logId = "[ModbusSinglePhaseActivePowerMeterDriver]: ";
	
	// a reference to the network driver
	private ModbusNetwork network;
	
	// a reference to the gateway driver
	private ModbusGatewayDriver gateway;
	
	// the list of driver instances currently connected to a device
	private Vector<ModbusSinglePhaseActivePowerMeterDriverInstance> connectedDrivers;
	
	// the registration object needed to handle the life span of this bundle in
	// the OSGi framework (it is a ServiceRegistration object for use by the
	// bundle registering the service to update the service's properties or to
	// unregister the service).
	private ServiceRegistration<?> regDriver;
	
	/**
	 * Create a driver instance (empty, will be initialized upon activation)
	 */
	public ModbusSinglePhaseActivePowerMeterDriver()
	{
		// do nothing here
	}
	
	/**
	 * Called when the bundle is activated. Takes care of initializing all the
	 * inner data structures and to register the provided services, unless the
	 * dynamic dependencies are not satisfied. In the last case, it will
	 * register provided services as soon as dependencies will be satisfied.
	 * 
	 * @param context
	 */
	public void activate(BundleContext context)
	{
		// init the logger
		this.logger = new DogLogInstance(context);
		
		// store the context
		this.context = context;
		
		// initialize the connected drivers list
		this.connectedDrivers = new Vector<ModbusSinglePhaseActivePowerMeterDriverInstance>();
		
		// try to register the service
		this.register();
	}
	
	public void deactivate()
	{
		// log deactivation
		this.logger.log(LogService.LOG_DEBUG, ModbusSinglePhaseActivePowerMeterDriver.logId + " Deactivation required");
		
		// unregister from the network driver
		for (ModbusSinglePhaseActivePowerMeterDriverInstance instance : this.connectedDrivers)
			this.network.removeDriver(instance);
		
		this.unRegister();
		
		// null the inner data structures
		this.context = null;
		this.logger = null;
		this.network = null;
		this.gateway = null;
		this.connectedDrivers = null;
	}
	
	/**
	 * Handles the "availability" of a Modbus network driver (store a reference
	 * to the driver and try to start).
	 * 
	 * @param netDriver
	 *            The available {@link ModbusNetwork} driver service.
	 */
	public void addedNetworkDriver(ModbusNetwork netDriver)
	{
		// store a reference to the network driver
		this.network = netDriver;
		
		// try to start service offering
		this.register();
	}
	
	/**
	 * Handles the removal of the connected network driver by unregistering the
	 * services provided by this driver
	 */
	public void removedNetworkDriver()
	{
		// un-register this service
		this.unRegister();
		
		// null the reference to the network driver
		this.network = null;
	}
	
	/**
	 * Handles the "availability" of a Modbus gateway driver (store a reference
	 * to the driver and try to start).
	 * 
	 * @param gwDriver
	 *            The available {@link ModbusGatewayDriver} service.
	 */
	public void addedGatewayDriver(ModbusGatewayDriver gwDriver)
	{
		// store a reference to the gateway driver
		this.gateway = gwDriver;
		
		// try to start service offering
		this.register();
	}
	
	/**
	 * Handles the removal of the connected network driver by unregistering the
	 * services provided by this driver
	 */
	public void removedGatewayDriver()
	{
		// un-register this service
		this.unRegister();
		
		// null the reference to the network driver
		this.gateway = null;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public int match(ServiceReference reference) throws Exception
	{
		int matchValue = Device.MATCH_NONE;
		
		if (this.regDriver != null)
		{
			// get the given device category
			String deviceCategory = (String) reference.getProperty(DogDeviceCostants.DEVICE_CATEGORY);
			
			// get the given device manufacturer
			String manifacturer = (String) reference.getProperty(DogDeviceCostants.MANUFACTURER);
			
			// get the gateway to which the device is connected
			@SuppressWarnings("unchecked")
			String gateway = (String) ((ControllableDevice) this.context.getService(reference)).getDeviceDescriptor()
					.getGateway();
			
			// compute the matching score between the given device and this
			// driver
			if (deviceCategory != null)
			{
				if (manifacturer != null
						&& (gateway != null)
						&& (manifacturer.equals(ModbusInfo.MANUFACTURER))
						&& (deviceCategory.equals(SinglePhaseActivePowerMeter.class.getName()) && (this.gateway
								.isGatewayAvailable(gateway))
						
						))
				{
					matchValue = SinglePhaseActivePowerMeter.MATCH_MANUFACTURER
							+ SinglePhaseActivePowerMeter.MATCH_TYPE;
				}
				
			}
		}
		return matchValue;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public String attach(ServiceReference reference) throws Exception
	{
		if (this.regDriver != null)
		{
			// get the gateway to which the device is connected
			String gateway = (String) ((ControllableDevice) this.context.getService(reference)).getDeviceDescriptor()
					.getGateway();
			
			// create a new driver instance
			ModbusSinglePhaseActivePowerMeterDriverInstance driverInstance = new ModbusSinglePhaseActivePowerMeterDriverInstance(
					network, (ControllableDevice) this.context.getService(reference), this.gateway.getSpecificGateway(
							gateway).getGatewayAddress(), this.gateway.getSpecificGateway(gateway).getGatewayPort(),
					this.gateway.getSpecificGateway(gateway).getGwProtocol(), this.context);
			
			((ControllableDevice) context.getService(reference)).setDriver(driverInstance);
			
			synchronized (this.connectedDrivers)
			{
				this.connectedDrivers.add(driverInstance);
			}
		}
		return null;
	}
	
	private void register()
	{
		if ((this.network != null) && (this.gateway != null) && (this.context != null) && (this.regDriver == null))
		{
			// create a new property object describing this driver
			Hashtable<String, Object> propDriver = new Hashtable<String, Object>();
			
			// add the id of this driver to the properties
			propDriver.put(DogDeviceCostants.DRIVER_ID, "Modbus_ModbusSinglePhaseActivePowerMeter_driver");
			
			// register this driver in the OSGi framework
			this.regDriver = this.context.registerService(Driver.class.getName(), this, propDriver);
		}
		
	}
	
	public void unRegister()
	{
		// un-registers this driver, if registered
		if (this.regDriver != null)
		{
			this.regDriver.unregister();
			this.regDriver = null;
		}
		
	}
	
}
