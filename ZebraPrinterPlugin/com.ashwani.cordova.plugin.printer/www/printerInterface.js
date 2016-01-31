function PrinterInterface() {
}

PrinterInterface.prototype.discoverPrinters = function(ipAddress, successCallback,
		errorCallback) {
	cordova.exec(successCallback, errorCallback, "PrinterInterface", 
			"discoverPrinters", [{"ipAddress":ipAddress}]);
};

PrinterInterface.prototype.connectWithPrinter = function(successCallback,
		errorCallback) {
	cordova.exec(successCallback, errorCallback, "PrinterInterface",
			"connectWithPrinter", [{}]);
};

PrinterInterface.prototype.print = function(ordersToPrint, successCallback, errorCallback) {
	cordova.exec(successCallback, errorCallback, "PrinterInterface", "print",
			[{"printdata": ordersToPrint}]);
};

PrinterInterface.install = function() {
	if (!window.plugins) {
		window.plugins = {};
	}

	window.plugins.printerInterface = new PrinterInterface();
	return window.plugins.printerInterface;
};

cordova.addConstructor(PrinterInterface.install);