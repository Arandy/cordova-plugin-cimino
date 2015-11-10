var 
	//argscheck = require('cordova/argscheck'),
	//utils = require('cordova/utils'),
	exec = require('cordova/exec');
    
var Cimino = function() {
        this.options = {};
};

Cimino.prototype = {
    /*
        Add your plugin methods here
    */
    init: function(success, error, args ){
        cordova.exec( success, error, "Cimino", "init", [args] );
    },

//    calibrate: function( success, error, args ) {
//        cordova.exec( success, error, "Cimino", "calibrate", [args] );
//    },
    
    capture: function( success, error, args ) {
        cordova.exec( success, error, "Cimino", "capture", [args] );
    },

    process: function( success, error, args ) {
        cordova.exec( success, error, "Cimino", "process", [args] );
    },

//    getMatchIndex: function( success, error, args) {
//        cordova.exec( success, error, "Cimino", "getMatchIndex", [args]);
//    },

//    getCropsFromImage:function( success, error, args ) {
//        console.log("args:"+JSON.stringify(args));
//        cordova.exec( success, error, "Cimino", "getCropsFromImage", [args]);
//    }

};

var CiminoInstance = new Cimino();

module.exports = CiminoInstance;