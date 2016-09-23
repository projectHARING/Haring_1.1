////////////////////////////////////////////////////////////////////////////////////////////////////
//
// FancyMeter
//
// * Minimal fancy looks for Server's meter via hacking the original ServerMeter
//
// by Darien Brito
// http://www.darienbrito.com
//
//

+ Server {
	fancyMeter { |numIns, numOuts, masterWin, posX, posY|
		^if( GUI.id == \swing and: { \JSCPeakMeter.asClass.notNil }, {
			\JSCPeakMeter.asClass.meterServer( this );
		}, { FancyServerMeter(this, numIns, numOuts, masterWin, posX, posY) });
	}
}
