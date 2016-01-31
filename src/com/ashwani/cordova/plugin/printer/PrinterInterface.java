package com.ashwani.cordova.plugin.printer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ashwani.cordova.plugin.printer.utils.DemoSleeper;
import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.printer.PrinterLanguage;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.DiscoveryException;
import com.zebra.sdk.printer.discovery.DiscoveryHandler;
import com.zebra.sdk.printer.discovery.NetworkDiscoverer;

public class PrinterInterface extends CordovaPlugin implements DiscoveryHandler {

	private static final String ACTION_DISCOVER_EVENT = "discoverPrinters";
	private static final String ACTION_CONNECT_EVENT = "connectWithPrinter";
	private static final String ACTION_PRINT_EVENT = "print";
	private static final String IPADDRESS = "ipAddress";
	private static final String SEATNUMBER = "seatNo";
	private static final String PAXNAME = "paxName";
	private static final String ORDERITEMS = "Items";

	private ArrayList<DiscoveredPrinter> printersFound = new ArrayList<DiscoveredPrinter>();
	private Connection printerConnection = null;
	private CallbackContext discoveryCallbackContext;
	private CallbackContext printerCallbackContext;

	@Override
	public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
		if (ACTION_DISCOVER_EVENT.equals(action)) {
			boolean isSuccess = true;
			JSONObject obj = args.optJSONObject(0);
			this.discoveryCallbackContext = callbackContext;
			if (obj != null) {
				printersFound.clear();
				String ipAddress = obj.optString(IPADDRESS);
				// TODO: check ipaddress format here
				discoverPrinters(ipAddress);
				JSONObject result = new JSONObject();
				result.put("DISCOVERY_STARTED", "Looking for printers in your subnet...");
				PluginResult pr = new PluginResult(Status.OK, result);
				pr.setKeepCallback(true);// Will keep the callback open
				if (this.discoveryCallbackContext != null) {
					this.discoveryCallbackContext.sendPluginResult(pr);
				}
				isSuccess = true;
			} else {
				JSONObject result = new JSONObject();
				result.put("MISSING_IP", "Please provide your device IP");
				PluginResult pr = new PluginResult(Status.ERROR, result);
				pr.setKeepCallback(false);// Will keep the callback open
				if (this.discoveryCallbackContext != null) {
					this.discoveryCallbackContext.sendPluginResult(pr);
				}
				this.discoveryCallbackContext = null;
				isSuccess = false;
			}
			return isSuccess;
		} else if (ACTION_CONNECT_EVENT.equals(action)) {

			return true;
		} else if (ACTION_PRINT_EVENT.equals(action)) {
			final JSONObject obj = args.optJSONObject(0);
			this.printerCallbackContext = callbackContext;
			boolean isSuccess = true;
			if (obj != null) {
				if (printersFound.size() == 0) {
					JSONObject result = new JSONObject();
					result.put("NO_PRINTER", "No printers available on your network, kindly restart printer dicsovery");
					PluginResult pr = new PluginResult(Status.ERROR, result);
					pr.setKeepCallback(false);// Will keep the callback open
					if (this.printerCallbackContext != null) {
						this.printerCallbackContext.sendPluginResult(pr);
					}
					isSuccess = false;
				} else {
					this.printerCallbackContext = callbackContext;
					cordova.getThreadPool().execute(new Runnable() {
						public void run() {
							try {
								ConnectwithPrinter(printersFound.get(0).getDiscoveryDataMap().get("ADDRESS"),
										printersFound.get(0).getDiscoveryDataMap().get("PORT_NUMBER"));
								JSONArray item = obj.getJSONArray("printdata");

								print(item);

								JSONObject result = new JSONObject();
								result.put("PRINT_SUCCESS", "Your print is ready");
								if (PrinterInterface.this.printerCallbackContext != null) {
									PrinterInterface.this.printerCallbackContext.success(result);
								}
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					});

					isSuccess = true;
				}
			} else {
				JSONObject result = new JSONObject();
				result.put("INVALID_DATA", "Oops! You forgot to provide data for printing");
				PluginResult pr = new PluginResult(Status.ERROR, result);
				pr.setKeepCallback(false);// Will keep the callback open
				if (this.printerCallbackContext != null) {
					this.printerCallbackContext.sendPluginResult(pr);
				}
				isSuccess = false;
			}
			return isSuccess;
		} else {
			JSONObject result = new JSONObject();
			result.put("INVALID_ACTION", "Sorry! Something went wrong while printing. Please give it another try. ");
			PluginResult pr = new PluginResult(Status.ERROR, result);
			pr.setKeepCallback(false);// Will keep the callback open
			callbackContext.sendPluginResult(pr);
			return false;
		}
	}

	public void discoverPrinters(String deviceIpAddress) throws JSONException {
		String subnetRange = deviceIpAddress.replaceAll("(.*\\.)\\d+$", "$1");
		subnetRange = subnetRange + "*";
		try {
			NetworkDiscoverer.subnetSearch(this, subnetRange);
		} catch (DiscoveryException e) {
			JSONObject result = new JSONObject();
			result.put("DISCOVERY_ERROR", "Sorry! Something went wrong during printer discovery.");
			PluginResult pr = new PluginResult(Status.ERROR, result);
			pr.setKeepCallback(false);// Will keep the callback open
			if (this.discoveryCallbackContext != null) {
				this.discoveryCallbackContext.sendPluginResult(pr);
			}
			this.discoveryCallbackContext = null;
		}
	}

	@Override
	public void discoveryError(String message) {
		JSONObject result = new JSONObject();
		try {
			result.put("DISCOVERY_ERROR", message);
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
		PluginResult pr = new PluginResult(Status.ERROR, result);
		pr.setKeepCallback(false);// Will keep the callback open
		if (this.discoveryCallbackContext != null) {
			this.discoveryCallbackContext.sendPluginResult(pr);
		}
		this.discoveryCallbackContext = null;
	}

	@Override
	public void discoveryFinished() {
		JSONObject result = new JSONObject();
		try {
			result.put("DISCOVERY_FINISHED", printersFound.size());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		PluginResult pr = new PluginResult(Status.OK, result);
		pr.setKeepCallback(false);// Will keep the callback open
		if (this.discoveryCallbackContext != null) {
			this.discoveryCallbackContext.sendPluginResult(pr);
		}
		this.discoveryCallbackContext = null;
	}

	@Override
	public void foundPrinter(final DiscoveredPrinter printerDetails) {
		printersFound.add(printerDetails);
		for (DiscoveredPrinter printer : printersFound) {
			System.out.println("Printer IP address: " + printer.getDiscoveryDataMap().get("ADDRESS"));
			System.out.println("Printer port: " + printer.getDiscoveryDataMap().get("PORT_NUMBER"));
		}
	}

	public void ConnectwithPrinter(String ipAddress, String portNumber) throws JSONException {
		try {
			printerConnection = new TcpConnection(ipAddress, Integer.valueOf(portNumber));
		} catch (NumberFormatException e) {
			return;
		}

		try {
			printerConnection.open();
			JSONObject result = new JSONObject();
			result.put("CONNECTION_SUCCESS", "Printer is connected now");
			PluginResult pr = new PluginResult(Status.OK, result);
			pr.setKeepCallback(true);// Will keep the callback open
			if (this.printerCallbackContext != null) {
				this.printerCallbackContext.sendPluginResult(pr);
			}
			// isSuccess = true;
		} catch (ConnectionException e) {
			JSONObject result = new JSONObject();
			result.put("CONNECTION_FAILURE", "Falied to connect with printer");
			PluginResult pr = new PluginResult(Status.ERROR, result);
			pr.setKeepCallback(false);// Will keep the callback open
			if (this.printerCallbackContext != null) {
				this.printerCallbackContext.sendPluginResult(pr);
			}
			this.printerCallbackContext = null;
		}
	}

	public void print(JSONArray item) throws JSONException {
		try {
			ZebraPrinter printer = null;
			if (printerConnection.isConnected()) {
				printer = ZebraPrinterFactory.getInstance(printerConnection);

				if (printer != null) {
					PrinterLanguage pl = printer.getPrinterControlLanguage();
					if (pl == PrinterLanguage.CPCL) {
						System.out.println("This demo will not work for CPCL printers!");
					} else {
						if (printer.getCurrentStatus().isPaperOut) {
							JSONObject result = new JSONObject();
							result.put("PAPER_OUT", "Printer is out of paper. Kindly put a new roll and then retry.");
							PluginResult pr = new PluginResult(Status.ERROR, result);
							pr.setKeepCallback(false);// Will keep the callback
														// open
							if (this.printerCallbackContext != null) {
								this.printerCallbackContext.sendPluginResult(pr);
							}
							this.printerCallbackContext = null;
						} else if (printer.getCurrentStatus().isReadyToPrint) {
							for (int i = 0; i < item.length(); i++) {
								JSONObject printObj = item.optJSONObject(i);
								String seatNo = printObj.optString(SEATNUMBER);
								String paxName = printObj.optString(PAXNAME);
								String orderData = printObj.optString(ORDERITEMS);
								System.out.println("seatNo " + seatNo + ", " + "PaxName " + paxName + ", "
										+ "orderData " + orderData);
								Map<String, String> printData = new HashMap<String, String>();
								printData.put("SEATNUMBER", seatNo);
								printData.put("PAXNAME", paxName);
								printData.put("ITEMS", orderData);
								sendTestLabel(printData);
							}
						} else if (printer.getCurrentStatus().isPaused) {
							JSONObject result = new JSONObject();
							result.put("PRINTER_PAUSED", "Cannot Print because the printer is paused.");
							PluginResult pr = new PluginResult(Status.ERROR, result);
							pr.setKeepCallback(false);// Will keep the callback
														// open
							if (this.printerCallbackContext != null) {
								this.printerCallbackContext.sendPluginResult(pr);
							}
							this.printerCallbackContext = null;
						} else if (printer.getCurrentStatus().isHeadOpen) {
							JSONObject result = new JSONObject();
							result.put("HEAD_OPEN", "Cannot Print because the printer head is open.");
							PluginResult pr = new PluginResult(Status.ERROR, result);
							pr.setKeepCallback(false);// Will keep the callback
														// open
							if (this.printerCallbackContext != null) {
								this.printerCallbackContext.sendPluginResult(pr);
							}
							this.printerCallbackContext = null;
						} else {
							System.out.println("Cannot Print.");
						}
					}
					printerConnection.close();
				}
			}
		} catch (ConnectionException e) {
			JSONObject result = new JSONObject();
			result.put("CONNECTION_FAILURE", "Falied to connect with printer");
			PluginResult pr = new PluginResult(Status.ERROR, result);
			pr.setKeepCallback(false);// Will keep the callback open
			if (this.printerCallbackContext != null) {
				this.printerCallbackContext.sendPluginResult(pr);
			}
			this.printerCallbackContext = null;
			System.out.println(e.getMessage());
		} catch (ZebraPrinterLanguageUnknownException e) {
			System.out.println("Could not detect printer language");
		} finally {
		}
	}

	private void sendTestLabel(Map<String, String> printData) throws JSONException {
		try {
			byte[] configLabel = createZplReceipt(printData).getBytes();
			printerConnection.write(configLabel);
			DemoSleeper.sleep(1500);
			if (printerConnection instanceof BluetoothConnection) {
				DemoSleeper.sleep(500);
			}
		} catch (ConnectionException e) {
			JSONObject result = new JSONObject();
			result.put("CONNECTION_FAILURE", "Falied to connect with printer");
			PluginResult pr = new PluginResult(Status.ERROR, result);
			pr.setKeepCallback(false);// Will keep the callback open
			if (this.printerCallbackContext != null) {
				this.printerCallbackContext.sendPluginResult(pr);
			}
			this.printerCallbackContext = null;
		}
	}

	private String createZplReceipt(Map<String, String> printData) {
		String paxName = printData.get("PAXNAME");
		String tmpHeader = "^XA" + "^PON^PW400^MNN^LL%d^LH0,0" + "\r\n" + "^FO 20,50" + "\r\n" + "^A0,N,70,70" + "\r\n"
				+ "^FD%s^FS" + "\r\n";
		System.out.println("print header 1 " + tmpHeader);
		int lastIndex = paxName.length() > 10 ? 10 : paxName.length();
		String firstSection = paxName.substring(0, lastIndex);
		String secondSection = "";
		if (lastIndex < paxName.length()) {
			secondSection = paxName.substring(lastIndex, paxName.length());
		}
		System.out.println(
				"print names " + firstSection + "-" + secondSection + "=" + lastIndex + "-" + paxName.length());
		int headerHeight = 160;
		if (secondSection != "") {
			// for displaying the name
			tmpHeader += "^FO 130,50" + "\r\n" + "^A0,N,35,35" + "\r\n" + "^FD%s^FS" + "\r\n" + "^FO 130,90" + "\r\n"
					+ "^A0,N,35,35" + "\r\n" + "^FD%s^FS" + "\r\n";// +
			headerHeight = 130;
		} else {
			tmpHeader += "^FO 130,50" + "\r\n" + "^A0,N,35,35" + "\r\n" + "^FD%s^FS" + "\r\n";// +
			headerHeight = 125;
		}
		System.out.println("print header 2 " + tmpHeader);
		String body = String.format("^LH0,%d", headerHeight);

		int heightOfOneLine = 30;

		String[] itemsToPrint = printData.get("ITEMS").split("#");
		int i = 0;
		int lineCount = 0;
		boolean isPriviousDouble = false;
		for (String productName : itemsToPrint) {
			int itemLastIndex = productName.length() > 30 ? 30 : productName.length();
			String itemFirstSection = productName.substring(0, itemLastIndex);
			String itemSecondSection = "";
			if (itemLastIndex < productName.length()) {
				itemSecondSection = productName.substring(itemLastIndex, productName.length());
			}
			System.out.println("print names " + firstSection + "-" + itemSecondSection + "=" + itemLastIndex + "-"
					+ paxName.length());
			int totalHeight = 0;

			if (itemSecondSection != "") {
				String lineItem = "^FO20,%d^GC10,10,B^FS" + "\r\n" + "^FO40,%d" + "\r\n" + "^A0,N,28,28" + "\r\n"
						+ "^FD%s^FS" + "\r\n" + "^FO40,%d" + "\r\n" + "^A0,N,28,28" + "\r\n" + "^FD%s^FS" + "\r\n";
				totalHeight = (i++ * heightOfOneLine);
				lineCount++;
				body += String.format(lineItem, totalHeight + 5, totalHeight, itemFirstSection, totalHeight + 30,
						itemSecondSection);
				isPriviousDouble = true;
				// totalHeight = totalHeight+30;
			} else {
				String lineItem = "^FO20,%d^GC10,10,B^FS" + "\r\n" + "^FO40,%d" + "\r\n" + "^A0,N,28,28" + "\r\n"
						+ "^FD%s^FS" + "\r\n";
				if (isPriviousDouble == true) {
					totalHeight = (i++ * heightOfOneLine) + 30;
				} else {
					totalHeight = i++ * heightOfOneLine;
				}
				body += String.format(lineItem, totalHeight + 5, totalHeight, itemFirstSection);
				isPriviousDouble = false;
			}
		}

		long totalBodyHeight = (itemsToPrint.length + lineCount + 1) * heightOfOneLine;

		long footerStartPosition = headerHeight + totalBodyHeight;

		String footer = String.format("^LH0,%d" + "\r\n" + "^FO20,1" + "\r\n" + "^A0N,30,30" + "\r\n"
				+ "^FD-------------------------------^FS" + "^XZ", footerStartPosition);

		long footerHeight = 30;
		long labelLength = headerHeight + totalBodyHeight + footerHeight;

		String header = "";
		if (secondSection == "") {
			header = String.format(tmpHeader, labelLength, printData.get("SEATNUMBER"), firstSection);
			System.out.println("header 1: " + firstSection);
		} else {
			header = String.format(tmpHeader, labelLength, printData.get("SEATNUMBER"), firstSection, secondSection);
			System.out.println("header 2: " + firstSection + ", " + secondSection);
		}

		String wholeZplLabel = String.format("%s%s%s", header, body, footer);

		return wholeZplLabel;
	}
}
