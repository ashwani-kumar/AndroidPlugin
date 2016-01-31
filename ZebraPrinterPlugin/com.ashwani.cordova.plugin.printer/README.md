# PhoneGap Version plugin
==========================
Cordova plugin for Zebra ZPL printers:

1. Dicsover printers:
	this.discoverPrinters = function(){
            var def = $q.defer();
            $rootScope.$emit('PRINTER_SEARCH');
            Config.printerDetails.printerCount = 0;
            if(!mobile.isCordovaEnabled) {
                def.resolve(false);
                return def.promise;
            }
            $window.plugins.printerInterface.discoverPrinters(mobile.ipAddress, function (result) {
                    console.log("discovery result " +
                        result);
                    var keys = Object.keys(result);
                    if(keys[0] == Config.printerMsgs.DISCOVERY_FINISHED){
                        Config.printerDetails.printerCount = result[Config.printerMsgs.DISCOVERY_FINISHED];
                        $rootScope.$emit('PRINTER_FOUND');
                        if(Config.printerDetails.printerCount == 0){
                            $timeout(function(){
                                $rootScope.showToast({timeout:3000, message:'Unable to find any printer in your subnet.'});
                            },100)
                        }else{
                            $timeout(function(){
                                $rootScope.showToast({timeout:3000, message:'Found printer.'});
                            },100)
                        }
                    }
                    return def.resolve(result);
                },
                function (error) {
                    console.log("discovery failed: " + error);
                    var keys = Object.keys(error);
                    if(keys[0] == Config.printerMsgs.DISCOVERY_ERROR){
                        //Reset printer count on discovery error;
                        $rootScope.$emit('PRINTER_FOUND');
                        Config.printerDetails.printerCount = 0;
                        $timeout(function(){
                            $rootScope.showToast({timeout:3000, message:'Unable to find any printer in your subnet.'});
                        },100)
                    }
                    return def.reject(error);
                }
            );
            return def.promise;
        }
        
2. Print Data:

Accepts array of objects.
[{
	seatNo: "76",
	paxName: "Mr. David",
	Items: "item a#item b# item c"
},{
	seatNo: "77",
	paxName: "Mrs. David",
	Items: "item a#item b# item c"
}]

this.connectAndPrint = function(ordersToPrint){
            var def = $q.defer();
            $window.plugins.printerInterface.print(ordersToPrint,
                function (result) {
                    console.log("print result" +
                        result);
                    $rootScope.$emit('PRINT_SUCCESS');
                    return def.resolve(result);
                },
                function (error) {
                    console.log("print failed: " + error);
                    var keys = Object.keys(error);
                    if(keys[0] == Config.printerMsgs.CONNECTION_FAILURE){
                        $rootScope.$emit('PRINT_ERROR');
                        $timeout(function(){
                            $rootScope.showToast({timeout:3000, message:'Unable to connect to printer.'});
                        },100)
                    }else if(keys[0] == Config.printerMsgs.PAPER_OUT){
                        $rootScope.$emit('PRINT_ERROR');
                        $timeout(function(){
                            $rootScope.showToast({timeout:3000, message:'Printer is out of paper.'});
                        },100)
                    }
                    return def.reject(error);
                }
            );
            return def.promise;
        } 
        
3. Initiate discovery from controller using:
$scope.discoverPrinters = function(){
        	CordovaService.discoverPrinters().then(function(result){
                console.log("Discovery Result: "+result);
                var keys = Object.keys(result);
                if(keys[0] == Config.printerMsgs.DISCOVERY_STARTED){
                	console.log("Discovery Result: "+result[Config.printerMsgs.DISCOVERY_STARTED]);
                }else if(keys[0] == Config.printerMsgs.DISCOVERY_FINISHED){
                	console.log("Discovery Result: "+result[Config.printerMsgs.DISCOVERY_FINISHED]);
                }
            }, function(err){
            	var keys = Object.keys(err);
            	if(keys[0] == Config.printerMsgs.MISSING_IP){
            		console.log("Discovery Result: "+result[Config.printerMsgs.MISSING_IP]);
            	}else if(keys[0] == Config.printerMsgs.DISCOVERY_ERROR){
            		console.log("Discovery Result: "+result[Config.printerMsgs.DISCOVERY_ERROR]);
            	}else if(keys[0] == Config.printerMsgs.INVALID_ACTION){
            		console.log("Discovery Result: "+result[Config.printerMsgs.INVALID_ACTION]);
            	}
                console.log(err);
            });
        }
        
4. Print from controller using:
$scope.printData = function(orderToPrint){
    	CordovaService.connectAndPrint(orderToPrint).then(function(result){
            console.log("Print Result: "+result);
            var keys = Object.keys(result);
            if(keys[0] == Config.printerMsgs.PRINT_SUCCESS){
            	console.log("Print Result: "+result[Config.printerMsgs.PRINT_SUCCESS]);
            }else if(keys[0] == Config.printerMsgs.CONNECTION_SUCCESS){
            	console.log("Print Result: "+result[Config.printerMsgs.CONNECTION_SUCCESS]);
            }
        }, function(err){
        	var keys = Object.keys(err);
        	if(keys[0] == Config.printerMsgs.NO_PRINTER){
        		console.log("Print Result: "+result[Config.printerMsgs.NO_PRINTER]);
        	}else if(keys[0] == Config.printerMsgs.INVALID_DATA){
        		console.log("Print Result: "+result[Config.printerMsgs.INVALID_DATA]);
        	}else if(keys[0] == Config.printerMsgs.INVALID_ACTION){
        		console.log("Print Result: "+result[Config.printerMsgs.INVALID_ACTION]);
        	}else if(keys[0] == Print.printerMsgs.CONNECTION_FAILURE){
        		console.log("Print Result: "+result[Config.printerMsgs.CONNECTION_FAILURE]);
        	}
            console.log(err);
        });
    }                

