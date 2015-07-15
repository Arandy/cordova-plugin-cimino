var cordova = require('cordova'),
    exec = require('cordova/exec');

var Cimino = function() {
        this.options = {};
};

Cimino.calibrate = function( success, error, args ) {
	console.log("calibrate");
        cordova.exec( success, error, "Cimino", "calibrate", args );
    };
    
Cimino.capture = function( success, error, args ) {
	console.log("capture");
        cordova.exec( success, error, "Cimino", "capture", args );
    };

var CiminoInstance = new Cimino();

module.exports = CiminoInstance;